package com.boxstockmanager.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.Log;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OcrScannerHelper {

    private static final String TAG = "OcrScannerHelper";
    private static Object tessInstance = null;
    private static boolean isTessInitialized = false;

    public static synchronized void initTesseractIfNeeded(Context context) {
        if (isTessInitialized && tessInstance != null) return;

        try {
            File filesDir = context.getFilesDir();
            File tessDir = new File(filesDir, "tessdata");
            if (!tessDir.exists()) {
                tessDir.mkdirs();
            }

            File trainedFile = new File(tessDir, "eng.traineddata");
            if (!trainedFile.exists() || trainedFile.length() == 0) {
                InputStream is = context.getAssets().open("tessdata/eng.traineddata");
                OutputStream os = new FileOutputStream(trainedFile);
                byte[] buffer = new byte[4096];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
                os.close();
                is.close();
            }

            try {
                Class<?> initClass = Class.forName("dev.ffmpegkit.tesseract.TesseractInitializer");
                Method initMethod = initClass.getMethod("init", Context.class);
                initMethod.invoke(null, context);
            } catch (Exception ignored) {}

            Class<?> ocrClass = null;
            try {
                ocrClass = Class.forName("dev.ffmpegkit.tesseract.TesseractOCR");
            } catch (ClassNotFoundException e) {
                try {
                    ocrClass = Class.forName("dev.ffmpegkit.tesseract.TesseractJNI");
                } catch (ClassNotFoundException ignored) {}
            }

            if (ocrClass != null) {
                try {
                    Constructor<?> ctor = ocrClass.getConstructor(String.class, String.class);
                    tessInstance = ctor.newInstance(filesDir.getAbsolutePath(), "eng");
                    isTessInitialized = true;
                } catch (Exception e1) {
                    try {
                        Constructor<?> ctor = ocrClass.getConstructor();
                        tessInstance = ctor.newInstance();
                        Method initM = ocrClass.getMethod("init", String.class, String.class);
                        Object res = initM.invoke(tessInstance, filesDir.getAbsolutePath(), "eng");
                        isTessInitialized = (res instanceof Boolean) ? (Boolean) res : true;
                    } catch (Exception e2) {
                        try {
                            Method staticInit = ocrClass.getMethod("init", String.class, String.class);
                            tessInstance = staticInit.invoke(null, filesDir.getAbsolutePath(), "eng");
                            isTessInitialized = true;
                        } catch (Exception ignored) {}
                    }
                }
            }

            if (isTessInitialized) {
                Log.d(TAG, "Tesseract initialized successfully.");
            } else {
                Log.w(TAG, "Tesseract instance could not be initialized, using pattern fallback.");
            }
        } catch (Throwable t) {
            Log.e(TAG, "Failed to initialize Tesseract: " + t.getMessage(), t);
            isTessInitialized = false;
        }
    }

    public static ProductScannerResult processBitmapUnified(Context context, Bitmap bitmap, MultiFormatReader reader, Map<DecodeHintType, Object> hints) {
        ProductScannerResult result = new ProductScannerResult();
        if (bitmap == null) return result;

        // 1. Decodes QR / Barcodes (0, 90, 180, 270 angles)
        Result zxingResult = decodeZXingMultiAngle(bitmap, reader, hints);
        if (zxingResult != null && zxingResult.getText() != null) {
            String decodedStr = zxingResult.getText().trim();
            parsePrintedProductInfo(decodedStr, result);

            if (result.getProductCode().isEmpty()) {
                if (!decodedStr.contains("\n") && !decodedStr.contains("\r") && !decodedStr.contains(":")) {
                    result.setProductCode(decodedStr);
                    if (result.getBarcode().isEmpty()) result.setBarcode(decodedStr);
                } else {
                    String firstLine = decodedStr.split("[\\r\\n|;]")[0].trim();
                    result.setProductCode(firstLine);
                    if (result.getBarcode().isEmpty()) result.setBarcode(firstLine);
                }
            }
        }

        // 2. Perform Offline OCR Preprocessing & Tesseract Text Extraction
        initTesseractIfNeeded(context);
        if (tessInstance != null) {
            try {
                Bitmap processedBmp = preprocessBitmapForOcr(bitmap);
                String ocrText = extractTextFromTesseract(tessInstance, processedBmp);
                if (ocrText != null && !ocrText.trim().isEmpty()) {
                    result.setRawOcrText(ocrText.trim());
                    parsePrintedProductInfo(ocrText, result);
                }
            } catch (Throwable t) {
                Log.e(TAG, "OCR Processing Error: " + t.getMessage(), t);
            }
        }

        return result;
    }

    private static String extractTextFromTesseract(Object instance, Bitmap bitmap) {
        if (instance == null || bitmap == null) return null;
        Class<?> clazz = instance.getClass();

        try {
            Method m = clazz.getMethod("recognize", Bitmap.class);
            Object res = m.invoke(instance, bitmap);
            if (res != null) {
                if (res instanceof String) return (String) res;
                try {
                    Method getTextM = res.getClass().getMethod("getText");
                    return (String) getTextM.invoke(res);
                } catch (Exception ignored) {}
                return res.toString();
            }
        } catch (Exception ignored) {}

        try {
            Method m = clazz.getMethod("recognizeText", Bitmap.class);
            Object res = m.invoke(instance, bitmap);
            if (res != null) return res.toString();
        } catch (Exception ignored) {}

        try {
            Method setImgM = clazz.getMethod("setImage", Bitmap.class);
            setImgM.invoke(instance, bitmap);
            try {
                Method getTxtM = clazz.getMethod("getUTF8Text");
                return (String) getTxtM.invoke(instance);
            } catch (Exception e1) {
                Method getTxtM = clazz.getMethod("getText");
                return (String) getTxtM.invoke(instance);
            }
        } catch (Exception ignored) {}

        return null;
    }

    private static Result decodeZXingMultiAngle(Bitmap original, MultiFormatReader reader, Map<DecodeHintType, Object> hints) {
        int[] angles = new int[]{0, 90, 180, 270};

        for (int angle : angles) {
            Bitmap bmpToUse = original;
            if (angle != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(angle);
                bmpToUse = Bitmap.createBitmap(original, 0, 0, original.getWidth(), original.getHeight(), matrix, true);
            }

            try {
                int w = bmpToUse.getWidth();
                int h = bmpToUse.getHeight();
                int[] pixels = new int[w * h];
                bmpToUse.getPixels(pixels, 0, w, 0, 0, w, h);

                LuminanceSource source = new RGBLuminanceSource(w, h, pixels);
                BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));
                Result r = reader.decode(binaryBitmap, hints);
                reader.reset();
                if (r != null) return r;
            } catch (Exception ignored) {
                reader.reset();
            }
        }

        return null;
    }

    public static Bitmap preprocessBitmapForOcr(Bitmap src) {
        if (src == null) return null;
        int width = src.getWidth();
        int height = src.getHeight();

        Bitmap bwBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bwBitmap);
        Paint paint = new Paint();

        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);

        float contrast = 1.35f;
        float translate = (-0.5f * contrast + 0.5f) * 255f;
        float[] contrastMatrix = new float[]{
                contrast, 0, 0, 0, translate,
                0, contrast, 0, 0, translate,
                0, 0, contrast, 0, translate,
                0, 0, 0, 1, 0
        };

        cm.postConcat(new ColorMatrix(contrastMatrix));
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(src, 0, 0, paint);

        return bwBitmap;
    }

    public static void parsePrintedProductInfo(String fullText, ProductScannerResult res) {
        if (fullText == null || fullText.trim().isEmpty()) return;

        String[] lines = fullText.split("[\\r\\n]+");
        List<String> tokens = new ArrayList<>();

        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            String[] subSegments = line.split("[|;]");
            for (String seg : subSegments) {
                String trimmedSeg = seg.trim();
                if (trimmedSeg.isEmpty()) continue;
                if (trimmedSeg.contains(",") && (trimmedSeg.contains(":") || trimmedSeg.contains("="))) {
                    String[] commaParts = trimmedSeg.split(",");
                    for (String cp : commaParts) {
                        if (!cp.trim().isEmpty()) tokens.add(cp.trim());
                    }
                } else {
                    tokens.add(trimmedSeg);
                }
            }
        }

        for (String token : tokens) {
            String label = null;
            String value = null;

            int colonIdx = token.indexOf(':');
            if (colonIdx > 0 && colonIdx < token.length() - 1) {
                label = token.substring(0, colonIdx).trim();
                value = token.substring(colonIdx + 1).trim();
            } else {
                int eqIdx = token.indexOf('=');
                if (eqIdx > 0 && eqIdx < token.length() - 1) {
                    label = token.substring(0, eqIdx).trim();
                    value = token.substring(eqIdx + 1).trim();
                } else {
                    int dashIdx = token.indexOf('-');
                    if (dashIdx > 0 && dashIdx < token.length() - 1) {
                        String left = token.substring(0, dashIdx).trim();
                        if (left.matches("(?i)^[a-z0-9\\s]{2,20}$") && !left.matches("^[0-9]+$")) {
                            label = left;
                            value = token.substring(dashIdx + 1).trim();
                        }
                    }
                }
            }

            if (label != null && value != null && !value.isEmpty()) {
                String normLabel = label.toLowerCase(Locale.US).replaceAll("[_\\-]", " ").replaceAll("\\s+", " ").trim();

                if (res.getProductName().isEmpty() &&
                    (normLabel.equals("product name") || normLabel.equals("productname") || normLabel.equals("name") || normLabel.equals("title"))) {
                    res.setProductName(value);
                    continue;
                }
                if (res.getBrand().isEmpty() &&
                    (normLabel.equals("brand") || normLabel.equals("brand name") || normLabel.equals("make") || normLabel.equals("manufacturer"))) {
                    res.setBrand(value);
                    continue;
                }
                if (res.getModel().isEmpty() &&
                    (normLabel.equals("model") || normLabel.equals("model no") || normLabel.equals("model number") || normLabel.equals("product model") || normLabel.equals("model name"))) {
                    res.setModel(value);
                    continue;
                }
                if (res.getProductCode().isEmpty() &&
                    (normLabel.equals("product code") || normLabel.equals("productcode") || normLabel.equals("code") || normLabel.equals("sku") || normLabel.equals("item code") || normLabel.equals("part no") || normLabel.equals("p/n"))) {
                    String cleanCode = value.split("[\\r\\n|;]")[0].trim();
                    res.setProductCode(cleanCode);
                    if (res.getBarcode().isEmpty()) res.setBarcode(cleanCode);
                    continue;
                }
                if (res.getImei1().isEmpty() &&
                    (normLabel.equals("imei 1") || normLabel.equals("imei1") || normLabel.equals("imei_1") || normLabel.equals("imei-1"))) {
                    res.setImei1(extractImeiFromValue(value));
                    continue;
                }
                if (res.getImei2().isEmpty() &&
                    (normLabel.equals("imei 2") || normLabel.equals("imei2") || normLabel.equals("imei_2") || normLabel.equals("imei-2"))) {
                    res.setImei2(extractImeiFromValue(value));
                    continue;
                }
                if (normLabel.equals("imei")) {
                    String imeiVal = extractImeiFromValue(value);
                    if (res.getImei1().isEmpty()) {
                        res.setImei1(imeiVal);
                    } else if (res.getImei2().isEmpty() && !imeiVal.equals(res.getImei1())) {
                        res.setImei2(imeiVal);
                    }
                    continue;
                }
                if (res.getCategory().isEmpty() &&
                    (normLabel.equals("category") || normLabel.equals("product category") || normLabel.equals("type") || normLabel.equals("device type"))) {
                    res.setCategory(value);
                    continue;
                }
                if (res.getColor().isEmpty() &&
                    (normLabel.equals("color") || normLabel.equals("colour") || normLabel.equals("shade"))) {
                    res.setColor(value);
                    continue;
                }
                if (res.getRam().isEmpty() &&
                    (normLabel.equals("ram") || normLabel.equals("memory") || normLabel.equals("system memory"))) {
                    res.setRam(value);
                    continue;
                }
                if (res.getStorage().isEmpty() &&
                    (normLabel.equals("storage") || normLabel.equals("rom") || normLabel.equals("internal memory") || normLabel.equals("capacity") || normLabel.equals("internal storage"))) {
                    res.setStorage(value);
                    continue;
                }
                if (res.getPurchasePrice() == 0.0 &&
                    (normLabel.equals("buy price") || normLabel.equals("purchase price") || normLabel.equals("cost price") || normLabel.equals("cost"))) {
                    res.setPurchasePrice(parseNumericDouble(value));
                    continue;
                }
                if (res.getSellingPrice() == 0.0 &&
                    (normLabel.equals("sell price") || normLabel.equals("selling price") || normLabel.equals("price") || normLabel.equals("mrp") || normLabel.equals("retail price"))) {
                    res.setSellingPrice(parseNumericDouble(value));
                    continue;
                }
                if (res.getStockQuantity() == 0 &&
                    (normLabel.equals("qty") || normLabel.equals("quantity") || normLabel.equals("stock") || normLabel.equals("stock qty") || normLabel.equals("initial stock") || normLabel.equals("count"))) {
                    res.setStockQuantity(parseNumericInt(value));
                    continue;
                }
                if (res.getSupplierName().isEmpty() &&
                    (normLabel.equals("supplier") || normLabel.equals("supplier name") || normLabel.equals("vendor") || normLabel.equals("vendor name") || normLabel.equals("distributor"))) {
                    res.setSupplierName(value);
                    continue;
                }
                if (res.getLocation().isEmpty() &&
                    (normLabel.equals("shelf") || normLabel.equals("rack") || normLabel.equals("location") || normLabel.equals("shelf location") || normLabel.equals("bin") || normLabel.equals("place"))) {
                    res.setLocation(value);
                    continue;
                }
                if (res.getDescription().isEmpty() &&
                    (normLabel.equals("description") || normLabel.equals("notes") || normLabel.equals("desc") || normLabel.equals("remark") || normLabel.equals("remarks") || normLabel.equals("details"))) {
                    res.setDescription(value);
                    continue;
                }
            }
        }

        // --- FALLBACK UNLABELED PATTERN RECOGNITION ---
        List<String> validImeis = new ArrayList<>();
        Pattern imeiPattern = Pattern.compile("\\b\\d{15}\\b");
        Matcher imeiMatcher = imeiPattern.matcher(fullText);
        while (imeiMatcher.find()) {
            String candidate = imeiMatcher.group();
            if (isValidLuhnImei(candidate) && !validImeis.contains(candidate)) {
                validImeis.add(candidate);
            }
        }

        if (res.getImei1().isEmpty() && validImeis.size() > 0) {
            res.setImei1(validImeis.get(0));
        }
        if (res.getImei2().isEmpty() && validImeis.size() > 1) {
            res.setImei2(validImeis.get(1));
        }

        if (res.getBrand().isEmpty()) {
            String[] knownBrands = new String[]{
                    "Apple", "Samsung", "Xiaomi", "Redmi", "Realme", "OnePlus",
                    "Vivo", "Oppo", "Poco", "Motorola", "Google", "Nokia",
                    "Infinix", "Tecno", "Sony", "Asus", "Honor", "Huawei"
            };
            for (String brand : knownBrands) {
                if (fullText.toLowerCase(Locale.US).contains(brand.toLowerCase(Locale.US))) {
                    res.setBrand(brand);
                    break;
                }
            }
        }

        if (res.getModel().isEmpty()) {
            Pattern modelCodePattern = Pattern.compile("\\b(SM-[A-Z0-9]+|M2[0-9]{3}[A-Z0-9]+|RMX[0-9]{4}|V2[0-9]{3}[A-Z0-9]+)\\b");
            Matcher m = modelCodePattern.matcher(fullText);
            if (m.find()) {
                res.setModel(m.group().trim());
            }
        }

        if (res.getRam().isEmpty()) {
            Pattern ramPattern = Pattern.compile("(?i)(\\b[0-9]{1,2}\\s*GB\\b|\\b[0-9]{1,2}GB\\b)\\s*RAM");
            Matcher m = ramPattern.matcher(fullText);
            if (m.find()) {
                res.setRam(m.group().trim());
            }
        }

        if (res.getStorage().isEmpty()) {
            Pattern storagePattern = Pattern.compile("(?i)(\\b[0-9]{2,4}\\s*GB\\b|\\b[0-9]{2,4}GB\\b|\\b1\\s*TB\\b)\\s*(storage|rom|internal memory)?");
            Matcher m = storagePattern.matcher(fullText);
            if (m.find()) {
                String matchStr = m.group().trim();
                if (!matchStr.equalsIgnoreCase(res.getRam())) {
                    res.setStorage(matchStr);
                }
            }
        }

        if (res.getColor().isEmpty()) {
            String[] colors = new String[]{
                    "Black", "White", "Blue", "Green", "Silver", "Gold", "Purple",
                    "Gray", "Grey", "Pink", "Red", "Yellow", "Graphite", "Midnight", "Starlight"
            };
            for (String c : colors) {
                if (fullText.toLowerCase(Locale.US).contains(c.toLowerCase(Locale.US))) {
                    res.setColor(c);
                    break;
                }
            }
        }
    }

    private static String extractImeiFromValue(String val) {
        if (val == null) return "";
        Pattern p = Pattern.compile("\\b\\d{15}\\b");
        Matcher m = p.matcher(val);
        if (m.find()) return m.group();
        return val.trim();
    }

    private static double parseNumericDouble(String val) {
        if (val == null) return 0.0;
        try {
            String clean = val.replaceAll("[^0-9.]", "");
            if (!clean.isEmpty()) return Double.parseDouble(clean);
        } catch (Exception ignored) {}
        return 0.0;
    }

    private static int parseNumericInt(String val) {
        if (val == null) return 0;
        try {
            String clean = val.replaceAll("[^0-9]", "");
            if (!clean.isEmpty()) return Integer.parseInt(clean);
        } catch (Exception ignored) {}
        return 0;
    }

    public static boolean isValidLuhnImei(String imei) {
        if (imei == null || imei.length() != 15) return false;
        int sum = 0;
        boolean alternate = false;
        for (int i = imei.length() - 1; i >= 0; i--) {
            int n = imei.charAt(i) - '0';
            if (n < 0 || n > 9) return false;
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }
}