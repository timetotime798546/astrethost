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

    /**
     * Initializes Tesseract OCR engine by copying language data assets to internal storage.
     * Target path: context.getFilesDir() + "/tesseract/tessdata/eng.traineddata".
     */
    public static synchronized void initTesseractIfNeeded(Context context) {
        if (isTessInitialized && tessInstance != null) return;

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
                Log.d(TAG, "Copying eng.traineddata asset to " + trainedFile.getAbsolutePath());
                InputStream is = null;
                try {
                    is = context.getAssets().open("tessdata/eng.traineddata");
                } catch (Exception e) {
                    Log.d(TAG, "tessdata/eng.traineddata not found, falling back to eng.traineddata");
                    is = context.getAssets().open("eng.traineddata");
                }

                OutputStream os = new FileOutputStream(trainedFile);
                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
                os.close();
                is.close();
                Log.d(TAG, "Successfully copied eng.traineddata (" + trainedFile.length() + " bytes)");
            } else {
                Log.d(TAG, "eng.traineddata already exists (" + trainedFile.length() + " bytes)");
            }

            String datapath = tessRootDir.getAbsolutePath();
            Log.d(TAG, "Initializing TessBaseAPI with datapath: " + datapath);

            try {
                com.googlecode.tesseract.android.TessBaseAPI baseApi = new com.googlecode.tesseract.android.TessBaseAPI();
                boolean success = baseApi.init(datapath, "eng");
                Log.d(TAG, "TessBaseAPI direct init result: " + success + " for datapath: " + datapath);

                if (success) {
                    tessInstance = baseApi;
                    isTessInitialized = true;
                } else {
                    Log.e(TAG, "TessBaseAPI direct init returned false for datapath: " + datapath);
                }
            } catch (Throwable t) {
                Log.e(TAG, "Direct TessBaseAPI instantiation failed, trying reflection: " + t.getMessage());
                try {
                    Class<?> tessClass = Class.forName("com.googlecode.tesseract.android.TessBaseAPI");
                    Object inst = tessClass.getConstructor().newInstance();
                    Method initM = tessClass.getMethod("init", String.class, String.class);
                    Object res = initM.invoke(inst, datapath, "eng");
                    boolean success = (res instanceof Boolean) ? (Boolean) res : true;
                    Log.d(TAG, "TessBaseAPI reflection init result: " + success);
                    if (success) {
                        tessInstance = inst;
                        isTessInitialized = true;
                    }
                } catch (Exception ex) {
                    Log.e(TAG, "Reflection init failed: " + ex.getMessage(), ex);
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "Unexpected error in initTesseractIfNeeded: " + t.getMessage(), t);
            isTessInitialized = false;
        }
    }

    /**
     * Executes Tesseract OCR using TessBaseAPI methods (setImage, getUTF8Text, clear).
     */
    public static synchronized String extractTextFromTesseract(Object instance, Bitmap bitmap) {
        if (instance == null || bitmap == null) return null;

        if (instance instanceof com.googlecode.tesseract.android.TessBaseAPI) {
            try {
                com.googlecode.tesseract.android.TessBaseAPI baseApi = (com.googlecode.tesseract.android.TessBaseAPI) instance;
                baseApi.setImage(bitmap);
                String rawText = baseApi.getUTF8Text();
                try {
                    baseApi.clear();
                } catch (Exception ignored) {}
                return rawText;
            } catch (Exception e) {
                Log.e(TAG, "Error executing TessBaseAPI.setImage/getUTF8Text: " + e.getMessage(), e);
            }
        }

        // Reflection fallback
        Class<?> clazz = instance.getClass();
        String text = null;
        try {
            Method setImageM = clazz.getMethod("setImage", Bitmap.class);
            setImageM.invoke(instance, bitmap);

            Method getUTF8M = clazz.getMethod("getUTF8Text");
            Object res = getUTF8M.invoke(instance);
            if (res != null) {
                text = res.toString();
            }

            try {
                Method clearM = clazz.getMethod("clear");
                clearM.invoke(instance);
            } catch (Exception ignored) {}
        } catch (Exception e) {
            Log.e(TAG, "Error invoking TessBaseAPI setImage/getUTF8Text via reflection: " + e.getMessage(), e);
        }

        return text;
    }

    /**
     * Unified processing pass combining ZXing barcode decoding with Tesseract OCR text extraction.
     */
    public static ProductScannerResult processBitmapUnified(Context context, Bitmap bitmap, MultiFormatReader reader, Map<DecodeHintType, Object> hints) {
        ProductScannerResult result = new ProductScannerResult();
        if (bitmap == null) return result;

        // 1. Decode Barcode/QR Code via ZXing multi-angle
        Result zxingResult = decodeZXingMultiAngle(bitmap, reader, hints);
        if (zxingResult != null && zxingResult.getText() != null) {
            String decodedStr = zxingResult.getText().trim();
            result.setProductCode(decodedStr);
            result.setBarcode(decodedStr);
        }

        // 2. Perform Offline OCR using TessBaseAPI
        initTesseractIfNeeded(context);
        if (tessInstance != null) {
            try {
                int[] angles = new int[]{0, 90, 270, 180};
                StringBuilder aggregatedText = new StringBuilder();

                for (int angle : angles) {
                    Bitmap rotatedBmp = bitmap;
                    if (angle != 0) {
                        Matrix matrix = new Matrix();
                        matrix.postRotate(angle);
                        rotatedBmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                    }

                    Bitmap processedBmp = preprocessBitmapForOcr(rotatedBmp);
                    String ocrText = extractTextFromTesseract(tessInstance, processedBmp);

                    if (ocrText != null && !ocrText.trim().isEmpty()) {
                        String cleanText = ocrText.trim();
                        aggregatedText.append(cleanText).append("\n");
                        parsePrintedProductInfo(cleanText, result);
                    }

                    if (!result.getImei1().isEmpty() && (!result.getModel().isEmpty() || !result.getBrand().isEmpty())) {
                        break;
                    }
                }

                String fullRaw = aggregatedText.toString().trim();
                if (!fullRaw.isEmpty()) {
                    result.setRawOcrText(fullRaw);
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

        float contrast = 1.45f;
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

        String[] rawLines = fullText.split("\n");
        List<String> lines = new ArrayList<>();
        for (String l : rawLines) {
            if (l != null && !l.trim().isEmpty()) {
                lines.add(l.trim());
            }
        }

        if (lines.isEmpty()) return;

        // --- 1. IMEI DETECTOR WITH LUHN VALIDATION ---
        List<String> validImeis = new ArrayList<>();
        Pattern digitSequencePattern = Pattern.compile("[0-9OoIil|SsBbZzGg]{14,18}");

        for (String line : lines) {
            Matcher m = digitSequencePattern.matcher(line);
            while (m.find()) {
                String candidate = cleanNumericOcrString(m.group());
                if (candidate.length() == 15 && isValidLuhnImei(candidate) && !validImeis.contains(candidate)) {
                    validImeis.add(candidate);
                }
            }
        }

        String imei1LabelPattern = "(?i)\\b(imei\\s*1|imei-1|imei1|imei\\s*slot\\s*1|imei\\s*\\(1\\)|imei/1)\\b";
        String imei2LabelPattern = "(?i)\\b(imei\\s*2|imei-2|imei2|imei\\s*slot\\s*2|imei\\s*\\(2\\)|imei/2)\\b";

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);

            if (Pattern.compile(imei1LabelPattern).matcher(line).find()) {
                String extracted = extractAdjacentValue(lines, i, imei1LabelPattern, true);
                String cleaned = cleanNumericOcrString(extracted);
                if (cleaned.length() == 15 && isValidLuhnImei(cleaned)) {
                    res.setImei1(cleaned);
                }
            }

            if (Pattern.compile(imei2LabelPattern).matcher(line).find()) {
                String extracted = extractAdjacentValue(lines, i, imei2LabelPattern, true);
                String cleaned = cleanNumericOcrString(extracted);
                if (cleaned.length() == 15 && isValidLuhnImei(cleaned)) {
                    res.setImei2(cleaned);
                }
            }
        }

        if (res.getImei1().isEmpty() && validImeis.size() > 0) {
            res.setImei1(validImeis.get(0));
        }
        if (res.getImei2().isEmpty() && validImeis.size() > 1) {
            if (!validImeis.get(1).equals(res.getImei1())) {
                res.setImei2(validImeis.get(1));
            }
        }

        // --- 2. BRAND PARSER ---
        String[] knownBrands = new String[]{
                "Apple", "Samsung", "Xiaomi", "Redmi", "Realme", "OnePlus",
                "Vivo", "Oppo", "Poco", "Motorola", "Google", "Nokia",
                "Infinix", "Tecno", "Sony", "Asus", "Honor", "Huawei",
                "iQOO", "Nothing", "Lenovo", "ZTE", "Meizu", "Lava", "Micromax"
        };
        for (String line : lines) {
            for (String brand : knownBrands) {
                if (line.toLowerCase(Locale.US).contains(brand.toLowerCase(Locale.US))) {
                    if (res.getBrand().isEmpty()) {
                        res.setBrand(brand);
                    }
                    break;
                }
            }
            if (!res.getBrand().isEmpty()) break;
        }

        if (res.getBrand().isEmpty()) {
            String brandLabelPattern = "(?i)\\b(brand|brand name|manufacturer|mfr|make)\\b";
            for (int i = 0; i < lines.size(); i++) {
                if (Pattern.compile(brandLabelPattern).matcher(lines.get(i)).find()) {
                    String val = extractAdjacentValue(lines, i, brandLabelPattern, false);
                    if (!val.isEmpty()) {
                        res.setBrand(val);
                        break;
                    }
                }
            }
        }

        // --- 3. MODEL PARSER ---
        String modelLabelPattern = "(?i)\\b(model\\s*no|model\\s*number|device\\s*model|product\\s*model|model\\s*name|mod|model)\\b";
        for (int i = 0; i < lines.size(); i++) {
            if (Pattern.compile(modelLabelPattern).matcher(lines.get(i)).find()) {
                String val = extractAdjacentValue(lines, i, modelLabelPattern, false);
                if (!val.isEmpty() && val.length() >= 2 && !val.equalsIgnoreCase("No") && !val.equalsIgnoreCase("Number")) {
                    res.setModel(val);
                    break;
                }
            }
        }

        if (res.getModel().isEmpty()) {
            Pattern modelCodePattern = Pattern.compile("\\b(SM-[A-Z0-9]+|M2[0-9]{3}[A-Z0-9]+|RMX[0-9]{4}|V2[0-9]{3}[A-Z0-9]+|CPH[0-9]{4}|TA-[0-9]{4})\\b");
            for (String line : lines) {
                Matcher m = modelCodePattern.matcher(line);
                if (m.find()) {
                    res.setModel(m.group().trim());
                    break;
                }
            }
        }

        // --- 4. SERIAL NUMBER ---
        String snLabelPattern = "(?i)\\b(s/n|sn|serial\\s*no|serial\\s*number|serial|serial#|ser\\s*no|service\\s*tag)\\b";
        for (int i = 0; i < lines.size(); i++) {
            if (Pattern.compile(snLabelPattern).matcher(lines.get(i)).find()) {
                String val = extractAdjacentValue(lines, i, snLabelPattern, false);
                if (!val.isEmpty() && val.length() >= 5) {
                    res.setSerialNumber(val);
                    break;
                }
            }
        }

        // --- 5. RAM & STORAGE ---
        for (int i = 0; i < lines.size(); i++) {
            Matcher m = Pattern.compile("(?i)\\b([0-9]{1,2}\\s*GB)\\s*(RAM)?\\b").matcher(lines.get(i));
            if (m.find()) {
                res.setRam(m.group(1).trim().toUpperCase(Locale.US));
                break;
            }
        }

        for (int i = 0; i < lines.size(); i++) {
            Matcher m = Pattern.compile("(?i)\\b([0-9]{2,4}\\s*GB|1\\s*TB)\\s*(ROM|Storage|Internal Memory)?\\b").matcher(lines.get(i));
            if (m.find()) {
                String matchStr = m.group(1).trim().toUpperCase(Locale.US);
                if (!matchStr.equalsIgnoreCase(res.getRam())) {
                    res.setStorage(matchStr);
                    break;
                }
            }
        }

        // --- 6. COLOR ---
        String colorLabelPattern = "(?i)\\b(color|colour|device\\s*color|device\\s*colour|shade)\\b";
        for (int i = 0; i < lines.size(); i++) {
            if (Pattern.compile(colorLabelPattern).matcher(lines.get(i)).find()) {
                String val = extractAdjacentValue(lines, i, colorLabelPattern, false);
                if (!val.isEmpty()) {
                    res.setColor(val);
                    break;
                }
            }
        }

        if (res.getColor().isEmpty()) {
            String[] knownColors = new String[]{
                    "Black", "White", "Blue", "Green", "Silver", "Gold", "Purple",
                    "Gray", "Grey", "Pink", "Red", "Yellow", "Graphite", "Midnight",
                    "Starlight", "Titanium", "Phantom Black", "Cream", "Violet", "Mint"
            };
            for (String line : lines) {
                for (String c : knownColors) {
                    if (line.toLowerCase(Locale.US).contains(c.toLowerCase(Locale.US))) {
                        res.setColor(c);
                        break;
                    }
                }
                if (!res.getColor().isEmpty()) break;
            }
        }
    }

    private static String extractAdjacentValue(List<String> lines, int index, String labelPatternRegex, boolean isNumericOnly) {
        if (index < 0 || index >= lines.size()) return "";
        String currentLine = lines.get(index);

        String[] parts = currentLine.split("(?i)" + labelPatternRegex);
        if (parts.length > 1) {
            String afterLabel = parts[1].replaceAll("^[:\\-=\\s/.]+", "").trim();
            if (!afterLabel.isEmpty()) {
                String firstToken = afterLabel.split("\\s+")[0].replaceAll("[:;,.]+$", "");
                if (!firstToken.isEmpty()) {
                    return isNumericOnly ? cleanNumericOcrString(firstToken) : firstToken;
                }
            }
        }

        if (parts.length > 0 && !parts[0].trim().isEmpty()) {
            String beforeLabel = parts[0].trim().replaceAll("[:\\-=\\s/.]+$", "");
            String[] beforeTokens = beforeLabel.split("\\s+");
            String lastToken = beforeTokens[beforeTokens.length - 1];
            if (!lastToken.isEmpty() && lastToken.length() >= 3) {
                return isNumericOnly ? cleanNumericOcrString(lastToken) : lastToken;
            }
        }

        if (index + 1 < lines.size()) {
            String lineBelow = lines.get(index + 1).trim().replaceAll("^[:\\-=\\s/.]+", "");
            if (!lineBelow.isEmpty()) {
                String firstToken = lineBelow.split("\\s+")[0].replaceAll("[:;,.]+$", "");
                if (!firstToken.isEmpty()) {
                    return isNumericOnly ? cleanNumericOcrString(firstToken) : firstToken;
                }
            }
        }

        if (index - 1 >= 0) {
            String lineAbove = lines.get(index - 1).trim().replaceAll("^[:\\-=\\s/.]+", "");
            if (!lineAbove.isEmpty()) {
                String firstToken = lineAbove.split("\\s+")[0].replaceAll("[:;,.]+$", "");
                if (!firstToken.isEmpty()) {
                    return isNumericOnly ? cleanNumericOcrString(firstToken) : firstToken;
                }
            }
        }

        return "";
    }

    public static String cleanNumericOcrString(String text) {
        if (text == null) return "";
        return text.replace('O', '0')
                   .replace('o', '0')
                   .replace('I', '1')
                   .replace('l', '1')
                   .replace('|', '1')
                   .replace('i', '1')
                   .replace('S', '5')
                   .replace('s', '5')
                   .replace('B', '8')
                   .replace('Z', '2')
                   .replace('z', '2')
                   .replace('G', '6')
                   .replace('g', '6')
                   .replaceAll("[^0-9]", "");
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