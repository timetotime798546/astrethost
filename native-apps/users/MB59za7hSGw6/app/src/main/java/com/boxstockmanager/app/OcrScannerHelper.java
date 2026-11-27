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
        if (isTessInitialized && tessInstance != null) {
            return;
        }

        try {
            File filesDir = context.getFilesDir();
            File tessRootDir = new File(filesDir, "tesseract");
            File tessDataDir = new File(tessRootDir, "tessdata");

            if (!tessDataDir.exists()) {
                boolean created = tessDataDir.mkdirs();
                Log.d(TAG, "Created tessdata directory: " + tessDataDir.getAbsolutePath() + " -> " + created);
            }

            File trainedFile = new File(tessDataDir, "eng.traineddata");
            if (!trainedFile.exists() || trainedFile.length() == 0) {
                Log.d(TAG, "Copying eng.traineddata from assets to " + trainedFile.getAbsolutePath());
                InputStream is = null;
                OutputStream os = null;
                try {
                    is = context.getAssets().open("tessdata/eng.traineddata");
                    os = new FileOutputStream(trainedFile);
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = is.read(buffer)) != -1) {
                        os.write(buffer, 0, read);
                    }
                    os.flush();
                    Log.d(TAG, "eng.traineddata copied successfully. File size: " + trainedFile.length() + " bytes.");
                } catch (Exception copyError) {
                    Log.e(TAG, "Failed to copy eng.traineddata from assets: " + copyError.getMessage(), copyError);
                } finally {
                    if (os != null) {
                        try { os.close(); } catch (Exception ignored) {}
                    }
                    if (is != null) {
                        try { is.close(); } catch (Exception ignored) {}
                    }
                }
            } else {
                Log.d(TAG, "eng.traineddata already exists (" + trainedFile.length() + " bytes).");
            }

            try {
                Class<?> tessClass = Class.forName("com.googlecode.tesseract.android.TessBaseAPI");
                Object apiInstance = tessClass.newInstance();

                Method initMethod = null;
                try {
                    initMethod = tessClass.getMethod("init", String.class, String.class);
                } catch (NoSuchMethodException e) {
                    Method[] methods = tessClass.getMethods();
                    for (Method m : methods) {
                        if ("init".equals(m.getName()) && m.getParameterTypes().length == 2) {
                            initMethod = m;
                            break;
                        }
                    }
                }

                if (initMethod != null) {
                    Object result = initMethod.invoke(apiInstance, tessRootDir.getAbsolutePath(), "eng");
                    boolean success = true;
                    if (result instanceof Boolean) {
                        success = (Boolean) result;
                    }
                    if (success) {
                        tessInstance = apiInstance;
                        isTessInitialized = true;
                        Log.i(TAG, "TessBaseAPI initialized successfully. Root dir: " + tessRootDir.getAbsolutePath() + " | Lang: eng");
                    } else {
                        Log.e(TAG, "TessBaseAPI.init returned false for datapath: " + tessRootDir.getAbsolutePath());
                    }
                } else {
                    Log.e(TAG, "Could not find init(String, String) on TessBaseAPI");
                }
            } catch (Throwable tessError) {
                Log.e(TAG, "Error initializing TessBaseAPI: " + tessError.getMessage(), tessError);
            }

        } catch (Throwable t) {
            Log.e(TAG, "initTesseractIfNeeded overall exception: " + t.getMessage(), t);
            isTessInitialized = false;
        }
    }

    public static ProductScannerResult processBitmapUnified(Context context, Bitmap bitmap, MultiFormatReader reader, Map<DecodeHintType, Object> hints) {
        ProductScannerResult result = new ProductScannerResult();
        if (bitmap == null) return result;

        // 1. Decode QR / Barcodes independently (0, 90, 180, 270 angles)
        Result zxingResult = decodeZXingMultiAngle(bitmap, reader, hints);
        if (zxingResult != null && zxingResult.getText() != null) {
            String decodedStr = zxingResult.getText().trim();
            result.setRawDecodedText(decodedStr);
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
        } else {
            Log.d(TAG, "QR Diagnostic: No QR Found in bitmap");
        }

        // 2. Perform Tesseract OCR independently on the SAME image regardless of QR status
        initTesseractIfNeeded(context);
        if (tessInstance != null) {
            try {
                // Try OCR on original bitmap first
                String ocrText = extractTextFromTesseract(tessInstance, bitmap);

                // If original bitmap yields no text or very short string, try preprocessed contrast bitmap
                if (ocrText == null || ocrText.trim().length() < 3) {
                    Bitmap processedBmp = preprocessBitmapForOcr(bitmap);
                    if (processedBmp != null) {
                        String ocrTextProcessed = extractTextFromTesseract(tessInstance, processedBmp);
                        if (ocrTextProcessed != null && ocrTextProcessed.trim().length() > (ocrText != null ? ocrText.trim().length() : 0)) {
                            ocrText = ocrTextProcessed;
                        }
                        processedBmp.recycle();
                    }
                }

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

        try {
            Class<?> clazz = instance.getClass();
            Method setImageMethod = clazz.getMethod("setImage", Bitmap.class);
            setImageMethod.invoke(instance, bitmap);

            String text = null;
            try {
                Method getUTF8TextMethod = clazz.getMethod("getUTF8Text");
                Object res = getUTF8TextMethod.invoke(instance);
                if (res != null) {
                    text = res.toString();
                }
            } catch (Exception e1) {
                try {
                    Method getTextMethod = clazz.getMethod("getText");
                    Object res = getTextMethod.invoke(instance);
                    if (res != null) {
                        text = res.toString();
                    }
                } catch (Exception ignored) {}
            }

            try {
                Method clearMethod = clazz.getMethod("clear");
                clearMethod.invoke(instance);
            } catch (Exception clearError) {
                Log.w(TAG, "Call to clear() failed: " + clearError.getMessage());
            }

            return text;
        } catch (Throwable t) {
            Log.e(TAG, "extractTextFromTesseract exception: " + t.getMessage(), t);
            return null;
        }
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
                
                if (bmpToUse != original) {
                    bmpToUse.recycle();
                }
                
                if (r != null) return r;
            } catch (Exception ignored) {
                reader.reset();
                if (bmpToUse != original) {
                    bmpToUse.recycle();
                }
            }
        }

        return null;
    }

    public static Bitmap preprocessBitmapForOcr(Bitmap src) {
        if (src == null) return null;
        try {
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
        } catch (Exception e) {
            Log.e(TAG, "Error during bitmap preprocessing: " + e.getMessage());
            return null;
        }
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
                    (normLabel.equals("product name") || normLabel.equals("productname") || normLabel.equals("name") || normLabel.equals("title") || normLabel.equals("item"))) {
                    res.setProductName(value);
                    continue;
                }
                if (res.getBrand().isEmpty() &&
                    (normLabel.equals("brand") || normLabel.equals("brand name") || normLabel.equals("make") || normLabel.equals("manufacturer"))) {
                    res.setBrand(value);
                    continue;
                }
                if (res.getModel().isEmpty() &&
                    (normLabel.equals("model") || normLabel.equals("model no") || normLabel.equals("model number") || normLabel.equals("product model") || normLabel.equals("model name") || normLabel.equals("mod"))) {
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
                    String imeiCandidate = extractImeiFromValue(value);
                    if (isValidLuhnImei(imeiCandidate)) {
                        res.setImei1(imeiCandidate);
                    }
                    continue;
                }
                if (res.getImei2().isEmpty() &&
                    (normLabel.equals("imei 2") || normLabel.equals("imei2") || normLabel.equals("imei_2") || normLabel.equals("imei-2"))) {
                    String imeiCandidate = extractImeiFromValue(value);
                    if (isValidLuhnImei(imeiCandidate)) {
                        res.setImei2(imeiCandidate);
                    }
                    continue;
                }
                if (normLabel.equals("imei") || normLabel.equals("imei no") || normLabel.equals("imei number")) {
                    String imeiVal = extractImeiFromValue(value);
                    if (isValidLuhnImei(imeiVal)) {
                        if (res.getImei1().isEmpty()) {
                            res.setImei1(imeiVal);
                        } else if (res.getImei2().isEmpty() && !imeiVal.equals(res.getImei1())) {
                            res.setImei2(imeiVal);
                        }
                    }
                    continue;
                }
                if (res.getSerialNumber().isEmpty() &&
                    (normLabel.equals("serial number") || normLabel.equals("serial no") || normLabel.equals("sn") || normLabel.equals("s/n") || normLabel.equals("serial") || normLabel.equals("ser no"))) {
                    res.setSerialNumber(value);
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

        if (res.getSerialNumber().isEmpty()) {
            Pattern snPattern = Pattern.compile("(?i)\\b(S/N|SN|Serial|Serial No|Serial Number)[:\\s]*([A-Z0-9]{6,20})\\b");
            Matcher snMatcher = snPattern.matcher(fullText);
            if (snMatcher.find()) {
                res.setSerialNumber(snMatcher.group(2));
            }
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
            Pattern modelCodePattern = Pattern.compile("\\b(SM-[A-Z0-9]+|M2[0-9]{3}[A-Z0-9]+|RMX[0-9]{4}|V2[0-9]{3}[A-Z0-9]+|CPH[0-9]{4}|iPhone\\s+[A-Z0-9\\s]+)\\b");
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
                    "Gray", "Grey", "Pink", "Red", "Yellow", "Graphite", "Midnight", "Starlight",
                    "Titanium", "Bronze", "Cyan"
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