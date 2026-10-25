package com.aabinstaller.app;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ApkSignerUtil {

    private static final String HEX_CHARS = "0123456789abcdef";

    public static String computeSha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < hash.length; i++) {
                int val = hash[i] & 0xFF;
                sb.append(HEX_CHARS.charAt(val >>> 4));
                sb.append(HEX_CHARS.charAt(val & 0x0F));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public static byte[] generateManifestMf(Map<String, String> fileDigests) {
        StringBuilder sb = new StringBuilder();
        sb.append("Manifest-Version: 1.0\r\n");
        sb.append("Created-By: 1.0 (AAB Native Installer)\r\n");
        sb.append("Built-By: AABInstallerEngine\r\n\r\n");

        for (Map.Entry<String, String> entry : fileDigests.entrySet()) {
            String name = entry.getKey();
            if (name.startsWith("META-INF/")) {
                continue;
            }
            sb.append("Name: ").append(name).append("\r\n");
            sb.append("SHA-256-Digest: ").append(entry.getValue()).append("\r\n\r\n");
        }
        return sb.toString().getBytes();
    }

    public static byte[] generateCertSf(byte[] manifestBytes, Map<String, String> fileDigests) {
        String manifestSha256 = computeSha256(manifestBytes);
        StringBuilder sb = new StringBuilder();
        sb.append("Signature-Version: 1.0\r\n");
        sb.append("Created-By: 1.0 (AAB Native Installer)\r\n");
        sb.append("SHA-256-Digest-Manifest: ").append(manifestSha256).append("\r\n\r\n");

        for (Map.Entry<String, String> entry : fileDigests.entrySet()) {
            String name = entry.getKey();
            if (name.startsWith("META-INF/")) {
                continue;
            }
            sb.append("Name: ").append(name).append("\r\n");
            sb.append("SHA-256-Digest: ").append(entry.getValue()).append("\r\n\r\n");
        }
        return sb.toString().getBytes();
    }

    public static byte[] generateTestCertRsa() {
        // Deterministic dummy test signature header block for Android standard test alignment
        byte[] testBlock = new byte[128];
        for (int i = 0; i < testBlock.length; i++) {
            testBlock[i] = (byte) ((i * 31 + 17) & 0xFF);
        }
        return testBlock;
    }
}