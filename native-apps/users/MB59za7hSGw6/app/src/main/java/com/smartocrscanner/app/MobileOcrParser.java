package com.smartocrscanner.app;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MobileOcrParser {

    private static final String[] BRANDS = {
            "APPLE", "IPHONE", "SAMSUNG", "GALAXY", "XIAOMI", "REDMI", "POCO",
            "REALME", "ONEPLUS", "VIVO", "OPPO", "GOOGLE", "PIXEL", "MOTOROLA",
            "MOTO", "NOKIA", "INFINIX", "TECNO", "ASUS", "NOTHING", "HONOR",
            "HUAWEI", "SONY", "IQOO"
    };

    private static final String[] COLORS = {
            "BLACK", "WHITE", "BLUE", "GREEN", "RED", "GOLD", "SILVER",
            "GREY", "GRAY", "PURPLE", "YELLOW", "PINK", "BRONZE", "MIDNIGHT",
            "STARLIGHT", "GRAPHITE", "SIERRA BLUE", "SPACE GRAY"
    };

    public static MobileModel parseTextToMobile(String rawText) {
        MobileModel model = new MobileModel();
        model.setRawOcrText(rawText);

        if (rawText == null || rawText.trim().isEmpty()) {
            return model;
        }

        String upperText = rawText.toUpperCase();

        // 1. Detect Brand
        for (String b : BRANDS) {
            if (upperText.contains(b)) {
                if (b.equals("IPHONE")) {
                    model.setBrand("Apple");
                } else if (b.equals("GALAXY")) {
                    model.setBrand("Samsung");
                } else if (b.equals("PIXEL")) {
                    model.setBrand("Google");
                } else if (b.equals("MOTO")) {
                    model.setBrand("Motorola");
                } else {
                    model.setBrand(capitalize(b));
                }
                break;
            }
        }

        // 2. Detect IMEIs (15 digit numbers)
        List<String> imeis = new ArrayList<String>();
        Pattern imeiPattern = Pattern.compile("\\b\\d{15}\\b");
        Matcher imeiMatcher = imeiPattern.matcher(rawText);
        while (imeiMatcher.find()) {
            String foundImei = imeiMatcher.group();
            if (!imeis.contains(foundImei)) {
                imeis.add(foundImei);
            }
        }
        if (imeis.size() >= 1) {
            model.setImei1(imeis.get(0));
        }
        if (imeis.size() >= 2) {
            model.setImei2(imeis.get(1));
        }

        // 3. Detect Serial Number
        Pattern snPattern = Pattern.compile("(?:S/N|SN|SERIAL|SERIAL NO|SER NO)[:\\s]*([A-Z0-9]{8,18})", Pattern.CASE_INSENSITIVE);
        Matcher snMatcher = snPattern.matcher(rawText);
        if (snMatcher.find()) {
            model.setSerialNumber(snMatcher.group(1).trim());
        }

        // 4. Detect RAM
        Pattern ramPattern = Pattern.compile("\\b(\\d{1,2})\\s*(?:GB|RAM)\\b", Pattern.CASE_INSENSITIVE);
        Matcher ramMatcher = ramPattern.matcher(rawText);
        if (ramMatcher.find()) {
            model.setRam(ramMatcher.group(1) + " GB");
        }

        // 5. Detect Storage
        Pattern storagePattern = Pattern.compile("\\b(16|32|64|128|256|512|1TB|2TB)\\s*(?:GB|TB|ROM|STORAGE)?\\b", Pattern.CASE_INSENSITIVE);
        Matcher storageMatcher = storagePattern.matcher(rawText);
        while (storageMatcher.find()) {
            String match = storageMatcher.group(1).toUpperCase();
            if (!match.equals(model.getRam().replace(" GB", ""))) {
                if (!match.contains("TB") && !match.contains("GB")) {
                    model.setStorage(match + " GB");
                } else {
                    model.setStorage(match);
                }
                break;
            }
        }

        // 6. Detect Color
        for (String c : COLORS) {
            if (upperText.contains(c)) {
                model.setColor(capitalize(c));
                break;
            }
        }

        // 7. Detect Model Name / Model Number
        Pattern modelNoPattern = Pattern.compile("(?:MODEL|MODEL NO|M/N|TYPE)[:\\s]*([A-Z0-9\\-_]{4,15})", Pattern.CASE_INSENSITIVE);
        Matcher modelNoMatcher = modelNoPattern.matcher(rawText);
        if (modelNoMatcher.find()) {
            model.setModelNumber(modelNoMatcher.group(1).trim());
        }

        // Extract first prominent non-IMEI line as Model title if empty
        String[] lines = rawText.split("\n");
        for (String line : lines) {
            String cleanLine = line.trim();
            if (cleanLine.length() > 3 && !cleanLine.matches(".*\\d{15}.*") && !cleanLine.toUpperCase().startsWith("IMEI")) {
                if (model.getModel().isEmpty()) {
                    model.setModel(cleanLine);
                    break;
                }
            }
        }

        // 8. Detect Product Code / SKU
        Pattern skuPattern = Pattern.compile("(?:SKU|CODE|PRODUCT CODE|ITEM)[:\\s]*([A-Z0-9\\-]{5,15})", Pattern.CASE_INSENSITIVE);
        Matcher skuMatcher = skuPattern.matcher(rawText);
        if (skuMatcher.find()) {
            model.setProductCode(skuMatcher.group(1).trim());
        }

        return model;
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}