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
import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OcrScannerHelper {

    private static final String TAG = "OcrScannerHelper";
    private static TessBaseAPI tessBaseAPI;
    private static boolean isTessInitialized = false;

    public static synchronized void initTesseractIfNeeded(Context context) {
        if (isTessInitialized && tessBaseAPI != null) return;

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

            tessBaseAPI = new TessBaseAPI();
            boolean success = tessBaseAPI.init(filesDir.getAbsolutePath(), "eng");
            if (success) {
                tessBaseAPI.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO);
                isTessInitialized = true;
                Log.d(TAG, "Tesseract 5.5.0 initialized successfully.");
            } else {
                Log.e(TAG, "Tesseract initialization returned false.");
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
            result.setProductCode(decodedStr);
            result.setBarcode(decodedStr);
        }

        // 2. Perform Offline OCR Preprocessing & Tesseract Text Extraction
        initTesseractIfNeeded(context);
        if (isTessInitialized && tessBaseAPI != null) {
            try {
                Bitmap processedBmp = preprocessBitmapForOcr(bitmap);
                tessBaseAPI.setImage(processedBmp);
                String ocrText = tessBaseAPI.getUTF8Text();
                if (ocrText != null) {
                    result.setRawOcrText(ocrText.trim());
                    parsePrintedProductInfo(ocrText, result);
                }
            } catch (Throwable t) {
                Log.e(TAG, "OCR Processing Error: " + t.getMessage(), t);
            }
        }

        return result;
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

        String[] lines = fullText.split("\n");

        // --- 1. IMEI DETECTOR WITH LUHN VALIDATION ---
        List<String> validImeis = new ArrayList<>();
        Pattern imeiPattern = Pattern.compile("\\b\\d{15}\\b");

        for (String line : lines) {
            Matcher m = imeiPattern.matcher(line);
            while (m.find()) {
                String candidate = m.group();
                if (isValidLuhnImei(candidate) && !validImeis.contains(candidate)) {
                    validImeis.add(candidate);
                }
            }
        }

        // Label-based IMEI mapping fallback
        for (String line : lines) {
            String lower = line.toLowerCase(Locale.US);
            if (lower.contains("imei 1") || lower.contains("imei1") || lower.contains("imei 1:")) {
                Matcher m = imeiPattern.matcher(line);
                if (m.find() && isValidLuhnImei(m.group())) {
                    res.setImei1(m.group());
                }
            } else if (lower.contains("imei 2") || lower.contains("imei2") || lower.contains("imei 2:")) {
                Matcher m = imeiPattern.matcher(line);
                if (m.find() && isValidLuhnImei(m.group())) {
                    res.setImei2(m.group());
                }
            }
        }

        if (res.getImei1().isEmpty() && validImeis.size() > 0) {
            res.setImei1(validImeis.get(0));
        }
        if (res.getImei2().isEmpty() && validImeis.size() > 1) {
            res.setImei2(validImeis.get(1));
        }

        // --- 2. BRAND PARSER ---
        String[] knownBrands = new String[]{
                "Apple", "Samsung", "Xiaomi", "Redmi", "Realme", "OnePlus",
                "Vivo", "Oppo", "Poco", "Motorola", "Google", "Nokia",
                "Infinix", "Tecno", "Sony", "Asus", "Honor", "Huawei"
        };
        for (String line : lines) {
            for (String brand : knownBrands) {
                if (line.toLowerCase(Locale.US).contains(brand.toLowerCase(Locale.US))) {
                    res.setBrand(brand);
                    break;
                }
            }
            if (!res.getBrand().isEmpty()) break;
        }

        // --- 3. MODEL PARSER ---
        Pattern modelPattern = Pattern.compile("(?i)(model|model no|model number|product model|model name)[:\\s]+([A-Z0-9\\-_]+)");
        for (String line : lines) {
            Matcher m = modelPattern.matcher(line);
            if (m.find() && m.groupCount() >= 2) {
                res.setModel(m.group(2).trim());
                break;
            }
        }
        if (res.getModel().isEmpty()) {
            Pattern modelCodePattern = Pattern.compile("\\b(SM-[A-Z0-9]+|M2[0-9]{3}[A-Z0-9]+|RMX[0-9]{4}|V2[0-9]{3}[A-Z0-9]+)\\b");
            for (String line : lines) {
                Matcher m = modelCodePattern.matcher(line);
                if (m.find()) {
                    res.setModel(m.group().trim());
                    break;
                }
            }
        }

        // --- 4. SERIAL NUMBER ---
        Pattern snPattern = Pattern.compile("(?i)(s/n|serial|serial no|serial number|sn)[:\\s]+([A-Z0-9]+)");
        for (String line : lines) {
            Matcher m = snPattern.matcher(line);
            if (m.find() && m.groupCount() >= 2) {
                res.setSerialNumber(m.group(2).trim());
                break;
            }
        }

        // --- 5. RAM & STORAGE ---
        Pattern ramPattern = Pattern.compile("(?i)(\\b[0-9]{1,2}\\s*GB\\b|\\b[0-9]{1,2}GB\\b)\\s*RAM");
        for (String line : lines) {
            Matcher m = ramPattern.matcher(line);
            if (m.find()) {
                res.setRam(m.group().trim());
                break;
            }
        }

        Pattern storagePattern = Pattern.compile("(?i)(\\b[0-9]{2,4}\\s*GB\\b|\\b[0-9]{2,4}GB\\b|\\b1\\s*TB\\b)\\s*(storage|rom|internal memory)?");
        for (String line : lines) {
            Matcher m = storagePattern.matcher(line);
            if (m.find()) {
                String matchStr = m.group().trim();
                if (!matchStr.equalsIgnoreCase(res.getRam())) {
                    res.setStorage(matchStr);
                    break;
                }
            }
        }

        // --- 6. COLOR ---
        String[] colors = new String[]{
                "Black", "White", "Blue", "Green", "Silver", "Gold", "Purple",
                "Gray", "Grey", "Pink", "Red", "Yellow", "Graphite", "Midnight", "Starlight"
        };
        for (String line : lines) {
            for (String c : colors) {
                if (line.toLowerCase(Locale.US).contains("color") || line.toLowerCase(Locale.US).contains("colour") || line.contains(c)) {
                    if (line.toLowerCase(Locale.US).contains(c.toLowerCase(Locale.US))) {
                        res.setColor(c);
                        break;
                    }
                }
            }
            if (!res.getColor().isEmpty()) break;
        }
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