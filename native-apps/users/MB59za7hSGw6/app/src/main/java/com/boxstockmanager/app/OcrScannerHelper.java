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
    private static Object tessOcrInstance = null;
    private static boolean isTessInitialized = false;

    /**
     * Initializes Tesseract OCR engine by copying language data assets to internal storage.
     * Target path: context.getFilesDir() + "/tesseract/tessdata/eng.traineddata".
     */
    public static synchronized void initTesseractIfNeeded(Context context) {
        if (isTessInitialized && tessOcrInstance != null) {
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
                Log.d(TAG, "Copying eng.traineddata asset to " + trainedFile.getAbsolutePath());
                InputStream is = null;
                try {
                    is = context.getAssets().open("tessdata/eng.traineddata");
                } catch (Exception e) {
                    Log.d(TAG, "tessdata/eng.traineddata not found, falling back to eng.traineddata");
                    try {
                        is = context.getAssets().open("eng.traineddata");
                    } catch (Exception ex) {
                        Log.e(TAG, "Could not open eng.traineddata asset: " + ex.getMessage());
                    }
                }

                if (is != null) {
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
                }
            } else {
                Log.d(TAG, "eng.traineddata already exists (" + trainedFile.length() + " bytes)");
            }

            String datapath = tessRootDir.getAbsolutePath();
            Log.d(TAG, "Initializing Tesseract with datapath: " + datapath);

            // Probe dev.ffmpegkit.tesseract.TesseractInitializer
            try {
                Class<?> initClass = Class.forName("dev.ffmpegkit.tesseract.TesseractInitializer");
                Log.d(TAG, "Found dev.ffmpegkit.tesseract.TesseractInitializer");
                Method[] initMethods = initClass.getDeclaredMethods();
                for (Method m : initMethods) {
                    try {
                        m.setAccessible(true);
                        Class<?>[] types = m.getParameterTypes();
                        if (types.length == 1 && types[0].isAssignableFrom(Context.class)) {
                            m.invoke(null, context);
                            Log.d(TAG, "Invoked TesseractInitializer." + m.getName() + "(Context)");
                        } else if (types.length == 2 && types[0] == String.class && types[1] == String.class) {
                            m.invoke(null, datapath, "eng");
                            Log.d(TAG, "Invoked TesseractInitializer." + m.getName() + "(String, String)");
                        }
                    } catch (Exception ignored) {}
                }
            } catch (ClassNotFoundException e) {
                Log.d(TAG, "TesseractInitializer class not found, skipping static init call.");
            }

            // Probe dev.ffmpegkit.tesseract.TesseractOCR
            try {
                Class<?> ocrClass = Class.forName("dev.ffmpegkit.tesseract.TesseractOCR");
                Log.d(TAG, "Found dev.ffmpegkit.tesseract.TesseractOCR");

                Object instance = null;

                Constructor<?>[] constructors = ocrClass.getConstructors();
                for (Constructor<?> c : constructors) {
                    try {
                        Class<?>[] pTypes = c.getParameterTypes();
                        if (pTypes.length == 0) {
                            instance = c.newInstance();
                            Log.d(TAG, "Instantiated TesseractOCR with default constructor");
                            break;
                        } else if (pTypes.length == 2 && pTypes[0] == String.class && pTypes[1] == String.class) {
                            instance = c.newInstance(datapath, "eng");
                            Log.d(TAG, "Instantiated TesseractOCR(datapath, eng)");
                            break;
                        } else if (pTypes.length == 1 && pTypes[0].isAssignableFrom(Context.class)) {
                            instance = c.newInstance(context);
                            Log.d(TAG, "Instantiated TesseractOCR(Context)");
                            break;
                        }
                    } catch (Exception ignored) {}
                }

                if (instance == null) {
                    try {
                        instance = ocrClass.getDeclaredConstructor().newInstance();
                    } catch (Exception ignored) {}
                }

                if (instance != null) {
                    Method[] methods = ocrClass.getDeclaredMethods();
                    for (Method m : methods) {
                        try {
                            String mName = m.getName().toLowerCase(Locale.US);
                            if (mName.contains("init") || mName.contains("create") || mName.contains("setup")) {
                                m.setAccessible(true);
                                Class<?>[] pTypes = m.getParameterTypes();
                                if (pTypes.length == 2 && pTypes[0] == String.class && pTypes[1] == String.class) {
                                    m.invoke(instance, datapath, "eng");
                                    Log.d(TAG, "Invoked TesseractOCR." + m.getName() + "(datapath, eng)");
                                } else if (pTypes.length == 1 && pTypes[0] == String.class) {
                                    m.invoke(instance, datapath);
                                    Log.d(TAG, "Invoked TesseractOCR." + m.getName() + "(datapath)");
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                    tessOcrInstance = instance;
                    isTessInitialized = true;
                    Log.d(TAG, "TesseractOCR initialized successfully.");
                } else {
                    Log.e(TAG, "Could not instantiate TesseractOCR");
                }
            } catch (ClassNotFoundException e) {
                Log.e(TAG, "dev.ffmpegkit.tesseract.TesseractOCR class not found", e);
            }

        } catch (Throwable t) {
            Log.e(TAG, "Unexpected error in initTesseractIfNeeded: " + t.getMessage(), t);
            isTessInitialized = false;
        }
    }

    /**
     * Executes Tesseract OCR using methods on dev.ffmpegkit.tesseract.TesseractOCR (recognize/getText).
     */
    public static synchronized String extractTextFromTesseract(Object instance, Bitmap bitmap) {
        if (bitmap == null) return null;
        if (instance == null) {
            try {
                Class<?> ocrClass = Class.forName("dev.ffmpegkit.tesseract.TesseractOCR");
                Method[] methods = ocrClass.getDeclaredMethods();
                for (Method m : methods) {
                    if (m.getParameterTypes().length == 1 && m.getParameterTypes()[0] == Bitmap.class) {
                        m.setAccessible(true);
                        Object res = m.invoke(null, bitmap);
                        String parsed = parseOcrResultObject(res);
                        if (parsed != null && !parsed.isEmpty()) return parsed;
                    }
                }
            } catch (Exception ignored) {}
            return null;
        }

        try {
            Class<?> clazz = instance.getClass();
            Method[] methods = clazz.getDeclaredMethods();

            for (Method m : methods) {
                String mName = m.getName().toLowerCase(Locale.US);
                if (mName.contains("recognize") || mName.contains("gettext") || mName.contains("utf8")
                        || mName.contains("process") || mName.contains("extract") || mName.contains("read")
                        || mName.contains("scan")) {
                    m.setAccessible(true);
                    Class<?>[] pTypes = m.getParameterTypes();
                    if (pTypes.length == 1 && pTypes[0] == Bitmap.class) {
                        Object resultObj = m.invoke(instance, bitmap);
                        String text = parseOcrResultObject(resultObj);
                        if (text != null && !text.trim().isEmpty()) {
                            return text.trim();
                        }
                    }
                }
            }

            for (Method m : methods) {
                m.setAccessible(true);
                Class<?>[] pTypes = m.getParameterTypes();
                if (pTypes.length == 1 && pTypes[0] == Bitmap.class) {
                    Object resultObj = m.invoke(instance, bitmap);
                    String text = parseOcrResultObject(resultObj);
                    if (text != null && !text.trim().isEmpty()) {
                        return text.trim();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error invoking OCR extraction via reflection: " + e.getMessage(), e);
        }

        return null;
    }

    private static String parseOcrResultObject(Object res) {
        if (res == null) return null;
        if (res instanceof String) {
            return (String) res;
        }

        try {
            Class<?> resClass = res.getClass();
            Method[] methods = resClass.getDeclaredMethods();
            for (Method m : methods) {
                String mName = m.getName().toLowerCase(Locale.US);
                if (mName.contains("text") || mName.contains("string") || mName.contains("utf8") || mName.contains("value")) {
                    if (m.getParameterTypes().length == 0 && m.getReturnType() == String.class) {
                        m.setAccessible(true);
                        String str = (String) m.invoke(res);
                        if (str != null && !str.trim().isEmpty()) {
                            return str.trim();
                        }
                    }
                }
            }

            for (Method m : methods) {
                if (m.getParameterTypes().length == 0 && Iterable.class.isAssignableFrom(m.getReturnType())) {
                    m.setAccessible(true);
                    Iterable<?> iterable = (Iterable<?>) m.invoke(res);
                    if (iterable != null) {
                        StringBuilder sb = new StringBuilder();
                        for (Object word : iterable) {
                            if (word != null) {
                                String wordStr = parseOcrResultObject(word);
                                if (wordStr != null) {
                                    sb.append(wordStr).append(" ");
                                }
                            }
                        }
                        if (sb.length() > 0) return sb.toString().trim();
                    }
                }
            }
        } catch (Exception ignored) {}

        String str = res.toString();
        if (str != null && !str.contains("@")) {
            return str;
        }

        return null;
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

        // 2. Perform Offline OCR using dev.ffmpegkit.tesseract engine
        initTesseractIfNeeded(context);
        if (tessOcrInstance != null || isTessInitialized) {
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
                    String ocrText = extractTextFromTesseract(tessOcrInstance, processedBmp);

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