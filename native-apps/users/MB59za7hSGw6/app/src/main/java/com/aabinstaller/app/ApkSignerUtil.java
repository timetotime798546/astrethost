package com.aabinstaller.app;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Date;
import java.util.Map;

public class ApkSignerUtil {

    private static final String HEX = "0123456789abcdef";
    private static KeyPair cachedKeyPair;
    private static byte[] cachedCertificate;

    public static synchronized void ensureKeys() {
        if (cachedKeyPair == null || cachedCertificate == null) {
            try {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                kpg.initialize(2048);
                cachedKeyPair = kpg.generateKeyPair();
                cachedCertificate = generateSelfSignedCertificate(cachedKeyPair);
            } catch (Exception e) {
                throw new RuntimeException("Key generation failed: " + e.getMessage(), e);
            }
        }
    }

    public static String computeSha256Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < hash.length; i++) {
                int val = hash[i] & 0xFF;
                sb.append(HEX.charAt(val >>> 4));
                sb.append(HEX.charAt(val & 0x0F));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public static byte[] computeSha256Bytes(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(data);
        } catch (Exception e) {
            return new byte[32];
        }
    }

    public static byte[] generateManifestMf(Map<String, String> fileDigests) {
        StringBuilder sb = new StringBuilder();
        sb.append("Manifest-Version: 1.0\r\n");
        sb.append("Created-By: 1.0 (Android AAB Installer)\r\n");
        sb.append("Built-By: AABInstallerEngine\r\n\r\n");

        for (Map.Entry<String, String> entry : fileDigests.entrySet()) {
            String name = entry.getKey();
            if (name.startsWith("META-INF/")) {
                continue;
            }
            sb.append("Name: ").append(name).append("\r\n");
            sb.append("SHA-256-Digest: ").append(entry.getValue()).append("\r\n\r\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] generateCertSf(byte[] manifestBytes, Map<String, String> fileDigests) {
        String manifestSha256 = computeSha256Hex(manifestBytes);
        StringBuilder sb = new StringBuilder();
        sb.append("Signature-Version: 1.0\r\n");
        sb.append("Created-By: 1.0 (Android AAB Installer)\r\n");
        sb.append("SHA-256-Digest-Manifest: ").append(manifestSha256).append("\r\n\r\n");

        for (Map.Entry<String, String> entry : fileDigests.entrySet()) {
            String name = entry.getKey();
            if (name.startsWith("META-INF/")) {
                continue;
            }
            sb.append("Name: ").append(name).append("\r\n");
            sb.append("SHA-256-Digest: ").append(entry.getValue()).append("\r\n\r\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] generateCertRsa(byte[] certSfBytes) {
        ensureKeys();
        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(cachedKeyPair.getPrivate());
            sig.update(certSfBytes);
            byte[] signatureBytes = sig.sign();

            return encodePkcs7SignedData(cachedCertificate, signatureBytes);
        } catch (Exception e) {
            return new byte[0];
        }
    }

    private static byte[] generateSelfSignedCertificate(KeyPair keyPair) throws Exception {
        ByteArrayOutputStream tbs = new ByteArrayOutputStream();

        // Version 3 [0] EXPLICIT INTEGER 2
        writeDerTagged(tbs, 0xA0, encodeDerInteger(BigInteger.valueOf(2)));
        // Serial Number
        tbs.write(encodeDerInteger(BigInteger.valueOf(System.currentTimeMillis())));
        // Signature Algorithm sha256WithRSAEncryption
        tbs.write(encodeAlgorithmId("1.2.840.113549.1.1.11"));
        // Issuer
        byte[] nameBytes = encodeDistinguishedName("Android Debug");
        tbs.write(nameBytes);
        // Validity (25 years)
        long now = System.currentTimeMillis();
        tbs.write(encodeValidity(new Date(now - 86400000L), new Date(now + 25L * 365 * 86400000L)));
        // Subject (same)
        tbs.write(nameBytes);
        // SubjectPublicKeyInfo
        tbs.write(keyPair.getPublic().getEncoded());

        byte[] tbsBytes = encodeDerSequence(tbs.toByteArray());

        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(keyPair.getPrivate());
        signer.update(tbsBytes);
        byte[] certSig = signer.sign();

        ByteArrayOutputStream certOut = new ByteArrayOutputStream();
        certOut.write(tbsBytes);
        certOut.write(encodeAlgorithmId("1.2.840.113549.1.1.11"));
        certOut.write(encodeDerBitString(certSig));

        return encodeDerSequence(certOut.toByteArray());
    }

    private static byte[] encodePkcs7SignedData(byte[] certDer, byte[] signatureBytes) throws Exception {
        // ContentInfo: signedData (1.2.840.113549.1.7.2)
        ByteArrayOutputStream signerInfo = new ByteArrayOutputStream();
        signerInfo.write(encodeDerInteger(BigInteger.valueOf(1))); // version

        // issuerAndSerialNumber
        ByteArrayOutputStream issuerAndSerial = new ByteArrayOutputStream();
        issuerAndSerial.write(encodeDistinguishedName("Android Debug"));
        issuerAndSerial.write(encodeDerInteger(BigInteger.valueOf(1)));
        signerInfo.write(encodeDerSequence(issuerAndSerial.toByteArray()));

        // digestAlgorithm sha-256 (2.16.840.1.101.3.4.2.1)
        signerInfo.write(encodeAlgorithmId("2.16.840.1.101.3.4.2.1"));
        // digestEncryptionAlgorithm rsaEncryption (1.2.840.113549.1.1.1)
        signerInfo.write(encodeAlgorithmId("1.2.840.113549.1.1.1"));
        // encryptedDigest
        signerInfo.write(encodeDerOctetString(signatureBytes));

        byte[] signerInfoSeq = encodeDerSequence(signerInfo.toByteArray());
        byte[] signerInfosSet = encodeDerSet(signerInfoSeq);

        ByteArrayOutputStream signedData = new ByteArrayOutputStream();
        signedData.write(encodeDerInteger(BigInteger.valueOf(1))); // version 1
        signedData.write(encodeDerSet(encodeAlgorithmId("2.16.840.1.101.3.4.2.1"))); // digestAlgorithms
        signedData.write(encodeDerSequence(encodeOid("1.2.840.113549.1.7.1"))); // data OID
        writeDerTagged(signedData, 0xA0, certDer); // certificates [0]
        signedData.write(signerInfosSet); // signerInfos

        byte[] signedDataSeq = encodeDerSequence(signedData.toByteArray());

        ByteArrayOutputStream contentInfo = new ByteArrayOutputStream();
        contentInfo.write(encodeOid("1.2.840.113549.1.7.2"));
        writeDerTagged(contentInfo, 0xA0, signedDataSeq);

        return encodeDerSequence(contentInfo.toByteArray());
    }

    public static void signApkWithSchemeV2(File apkFile) throws Exception {
        ensureKeys();
        RandomAccessFile raf = new RandomAccessFile(apkFile, "rw");
        try {
            long fileLength = raf.length();
            if (fileLength < 22) {
                return;
            }

            // Find End of Central Directory (EOCD)
            long eocdOffset = -1;
            long searchStart = Math.max(0, fileLength - 65557);
            byte[] searchBuffer = new byte[(int) (fileLength - searchStart)];
            raf.seek(searchStart);
            raf.readFully(searchBuffer);

            for (int i = searchBuffer.length - 22; i >= 0; i--) {
                if (searchBuffer[i] == 0x50 && searchBuffer[i + 1] == 0x4b && searchBuffer[i + 2] == 0x05 && searchBuffer[i + 3] == 0x06) {
                    eocdOffset = searchStart + i;
                    break;
                }
            }

            if (eocdOffset == -1) {
                return;
            }

            raf.seek(eocdOffset + 12);
            int cdSize = Integer.reverseBytes(raf.readInt());
            int cdOffset = Integer.reverseBytes(raf.readInt());

            // Compute APK Merkle Digest (1MB chunked digest over entries, central directory, and adjusted EOCD)
            byte[] eocdBytes = new byte[(int) (fileLength - eocdOffset)];
            raf.seek(eocdOffset);
            raf.readFully(eocdBytes);

            byte[] adjustedEocd = new byte[eocdBytes.length];
            System.arraycopy(eocdBytes, 0, adjustedEocd, 0, eocdBytes.length);

            // Compute content digest
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[65536];

            // 1. Before central directory
            raf.seek(0);
            long bytesRemaining = cdOffset;
            while (bytesRemaining > 0) {
                int toRead = (int) Math.min(bytesRemaining, buf.length);
                int r = raf.read(buf, 0, toRead);
                if (r == -1) break;
                md.update(buf, 0, r);
                bytesRemaining -= r;
            }

            // 2. Central directory
            raf.seek(cdOffset);
            bytesRemaining = cdSize;
            while (bytesRemaining > 0) {
                int toRead = (int) Math.min(bytesRemaining, buf.length);
                int r = raf.read(buf, 0, toRead);
                if (r == -1) break;
                md.update(buf, 0, r);
                bytesRemaining -= r;
            }

            // 3. EOCD
            md.update(adjustedEocd);
            byte[] overallDigest = md.digest();

            // Construct APK Signature Scheme v2 block
            byte[] signedData = buildV2SignedData(overallDigest, cachedCertificate);
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(cachedKeyPair.getPrivate());
            sig.update(signedData);
            byte[] signatureBytes = sig.sign();

            byte[] v2SignerBlock = buildV2SignerBlock(signedData, signatureBytes, cachedKeyPair.getPublic());
            byte[] v2BlockPayload = buildLengthPrefixedSequence(v2SignerBlock);

            // APK Signing Block layout:
            // uint64 size_of_block
            // uint64 pair_length, uint32 ID (0x7109871a), v2BlockPayload
            // uint64 size_of_block
            // 16 bytes magic "APK Sig Block 42"
            int pairLen = 4 + v2BlockPayload.length;
            long blockSize = 8L + 8L + pairLen + 16L;

            ByteArrayOutputStream blockStream = new ByteArrayOutputStream();
            writeUint64LE(blockStream, blockSize);
            writeUint64LE(blockStream, pairLen);
            writeUint32LE(blockStream, 0x7109871a);
            blockStream.write(v2BlockPayload);
            writeUint64LE(blockStream, blockSize);
            blockStream.write("APK Sig Block 42".getBytes(StandardCharsets.US_ASCII));

            byte[] apkSigningBlock = blockStream.toByteArray();

            // Read CD and EOCD to memory
            byte[] cdData = new byte[cdSize];
            raf.seek(cdOffset);
            raf.readFully(cdData);

            // Update EOCD Central Directory offset to point to after signing block
            int newCdOffset = cdOffset + apkSigningBlock.length;
            adjustedEocd[16] = (byte) (newCdOffset & 0xFF);
            adjustedEocd[17] = (byte) ((newCdOffset >>> 8) & 0xFF);
            adjustedEocd[18] = (byte) ((newCdOffset >>> 16) & 0xFF);
            adjustedEocd[19] = (byte) ((newCdOffset >>> 24) & 0xFF);

            // Write APK Signing Block, followed by CD and adjusted EOCD
            raf.seek(cdOffset);
            raf.write(apkSigningBlock);
            raf.write(cdData);
            raf.write(adjustedEocd);
            raf.setLength(raf.getFilePointer());

        } finally {
            raf.close();
        }
    }

    private static byte[] buildV2SignedData(byte[] apkDigest, byte[] certBytes) throws Exception {
        ByteArrayOutputStream digestsOut = new ByteArrayOutputStream();
        ByteArrayOutputStream digestEntry = new ByteArrayOutputStream();
        writeUint32LE(digestEntry, 0x0103); // SHA256withRSA
        digestEntry.write(buildLengthPrefixedBytes(apkDigest));
        digestsOut.write(buildLengthPrefixedBytes(digestEntry.toByteArray()));

        ByteArrayOutputStream certsOut = new ByteArrayOutputStream();
        certsOut.write(buildLengthPrefixedBytes(certBytes));

        ByteArrayOutputStream signedData = new ByteArrayOutputStream();
        signedData.write(buildLengthPrefixedBytes(digestsOut.toByteArray()));
        signedData.write(buildLengthPrefixedBytes(certsOut.toByteArray()));
        signedData.write(buildLengthPrefixedBytes(new byte[0])); // empty additional attributes

        return signedData.toByteArray();
    }

    private static byte[] buildV2SignerBlock(byte[] signedData, byte[] signature, PublicKey publicKey) throws Exception {
        ByteArrayOutputStream signaturesOut = new ByteArrayOutputStream();
        ByteArrayOutputStream sigEntry = new ByteArrayOutputStream();
        writeUint32LE(sigEntry, 0x0103);
        sigEntry.write(buildLengthPrefixedBytes(signature));
        signaturesOut.write(buildLengthPrefixedBytes(sigEntry.toByteArray()));

        ByteArrayOutputStream signer = new ByteArrayOutputStream();
        signer.write(buildLengthPrefixedBytes(signedData));
        signer.write(buildLengthPrefixedBytes(signaturesOut.toByteArray()));
        signer.write(buildLengthPrefixedBytes(publicKey.getEncoded()));

        return signer.toByteArray();
    }

    private static byte[] buildLengthPrefixedBytes(byte[] data) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeUint32LE(out, data.length);
        out.write(data);
        return out.toByteArray();
    }

    private static byte[] buildLengthPrefixedSequence(byte[] data) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeUint32LE(out, data.length + 4);
        out.write(buildLengthPrefixedBytes(data));
        return out.toByteArray();
    }

    private static void writeUint32LE(ByteArrayOutputStream out, int value) {
        out.write(value & 0xFF);
        out.write((value >>> 8) & 0xFF);
        out.write((value >>> 16) & 0xFF);
        out.write((value >>> 24) & 0xFF);
    }

    private static void writeUint64LE(ByteArrayOutputStream out, long value) {
        for (int i = 0; i < 8; i++) {
            out.write((int) ((value >>> (i * 8)) & 0xFF));
        }
    }

    // ASN.1 DER Encoding Helpers
    private static byte[] encodeDerSequence(byte[] data) throws Exception {
        return wrapDerHeader(0x30, data);
    }

    private static byte[] encodeDerSet(byte[] data) throws Exception {
        return wrapDerHeader(0x31, data);
    }

    private static byte[] encodeDerOctetString(byte[] data) throws Exception {
        return wrapDerHeader(0x04, data);
    }

    private static byte[] encodeDerBitString(byte[] data) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0x00); // 0 unused bits
        out.write(data);
        return wrapDerHeader(0x03, out.toByteArray());
    }

    private static byte[] encodeDerInteger(BigInteger val) throws Exception {
        return wrapDerHeader(0x02, val.toByteArray());
    }

    private static byte[] wrapDerHeader(int tag, byte[] data) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(tag);
        int len = data.length;
        if (len < 128) {
            out.write(len);
        } else if (len <= 0xFF) {
            out.write(0x81);
            out.write(len);
        } else if (len <= 0xFFFF) {
            out.write(0x82);
            out.write((len >>> 8) & 0xFF);
            out.write(len & 0xFF);
        } else {
            out.write(0x83);
            out.write((len >>> 16) & 0xFF);
            out.write((len >>> 8) & 0xFF);
            out.write(len & 0xFF);
        }
        out.write(data);
        return out.toByteArray();
    }

    private static void writeDerTagged(ByteArrayOutputStream out, int tag, byte[] data) throws Exception {
        out.write(wrapDerHeader(tag, data));
    }

    private static byte[] encodeAlgorithmId(String oidStr) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(encodeOid(oidStr));
        out.write(0x05); // NULL
        out.write(0x00);
        return encodeDerSequence(out.toByteArray());
    }

    private static byte[] encodeOid(String oidStr) throws Exception {
        String[] parts = oidStr.split("\\.");
        int first = Integer.parseInt(parts[0]);
        int second = Integer.parseInt(parts[1]);

        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write((first * 40) + second);

        for (int i = 2; i < parts.length; i++) {
            long val = Long.parseLong(parts[i]);
            byte[] enc = encodeOidValue(val);
            body.write(enc);
        }

        return wrapDerHeader(0x06, body.toByteArray());
    }

    private static byte[] encodeOidValue(long val) {
        if (val == 0) return new byte[]{0};
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int count = 0;
        long tmp = val;
        while (tmp > 0) {
            count++;
            tmp >>>= 7;
        }
        byte[] res = new byte[count];
        for (int i = count - 1; i >= 0; i--) {
            int b = (int) (val & 0x7F);
            if (i != count - 1) b |= 0x80;
            res[i] = (byte) b;
            val >>>= 7;
        }
        return res;
    }

    private static byte[] encodeDistinguishedName(String commonName) throws Exception {
        ByteArrayOutputStream attr = new ByteArrayOutputStream();
        attr.write(encodeOid("2.5.4.3")); // CN
        attr.write(wrapDerHeader(0x0C, commonName.getBytes(StandardCharsets.UTF_8))); // UTF8String

        byte[] seq = encodeDerSequence(attr.toByteArray());
        byte[] set = encodeDerSet(seq);
        return encodeDerSequence(set);
    }

    private static byte[] encodeValidity(Date notBefore, Date notAfter) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(wrapDerHeader(0x17, formatDateUtc(notBefore).getBytes(StandardCharsets.US_ASCII)));
        out.write(wrapDerHeader(0x17, formatDateUtc(notAfter).getBytes(StandardCharsets.US_ASCII)));
        return encodeDerSequence(out.toByteArray());
    }

    private static String formatDateUtc(Date date) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyMMddHHmmss'Z'", java.util.Locale.US);
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        return sdf.format(date);
    }
}