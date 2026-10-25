package com.aabinstaller.app;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class BinaryXmlUtil {

    public static final short RES_XML_TYPE = 0x0003;
    public static final short RES_STRING_POOL_TYPE = 0x0001;
    public static final short RES_XML_RESOURCE_MAP_TYPE = 0x0180;
    public static final short RES_XML_START_NAMESPACE_TYPE = 0x0100;
    public static final short RES_XML_END_NAMESPACE_TYPE = 0x0101;
    public static final short RES_XML_START_ELEMENT_TYPE = 0x0102;
    public static final short RES_XML_END_ELEMENT_TYPE = 0x0103;

    public static boolean isBinaryXml(byte[] data) {
        if (data == null || data.length < 8) {
            return false;
        }
        int magic = (data[0] & 0xFF) | ((data[1] & 0xFF) << 8);
        return magic == RES_XML_TYPE;
    }

    public static void inspectManifestParameters(byte[] manifestData, AabBundleInfo info) {
        if (manifestData == null || manifestData.length == 0) {
            return;
        }

        if (isBinaryXml(manifestData)) {
            info.setManifestBinary(true);
            parseStringsFromBinaryXml(manifestData, info);
        } else {
            info.setManifestBinary(false);
            parseStringsFromRawOrProto(manifestData, info);
        }
    }

    private static void parseStringsFromBinaryXml(byte[] data, AabBundleInfo info) {
        try {
            int stringPoolOffset = 8;
            if (data.length > stringPoolOffset + 28) {
                int stringCount = getInt32(data, stringPoolOffset + 8);
                int stringsStart = stringPoolOffset + getInt32(data, stringPoolOffset + 20);
                boolean isUtf8 = (getInt32(data, stringPoolOffset + 16) & (1 << 8)) != 0;

                int offsetTable = stringPoolOffset + 28;
                for (int i = 0; i < stringCount && offsetTable + (i * 4) + 4 <= stringsStart; i++) {
                    int strOffset = stringsStart + getInt32(data, offsetTable + (i * 4));
                    String str = readStringAt(data, strOffset, isUtf8);
                    detectPackageAndSdk(str, info);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static String readStringAt(byte[] data, int offset, boolean isUtf8) {
        if (offset < 0 || offset >= data.length) return "";
        try {
            if (isUtf8) {
                int len = data[offset] & 0xFF;
                if ((len & 0x80) != 0) {
                    offset += 2;
                } else {
                    offset += 1;
                }
                int byteLen = data[offset] & 0xFF;
                if ((byteLen & 0x80) != 0) {
                    offset += 2;
                } else {
                    offset += 1;
                }
                if (offset + byteLen <= data.length) {
                    return new String(data, offset, byteLen, StandardCharsets.UTF_8);
                }
            } else {
                int len = (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
                offset += 2;
                if (offset + (len * 2) <= data.length) {
                    return new String(data, offset, len * 2, StandardCharsets.UTF_16LE);
                }
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private static void parseStringsFromRawOrProto(byte[] data, AabBundleInfo info) {
        try {
            StringBuilder current = new StringBuilder();
            List<String> extracted = new ArrayList<String>();
            for (int i = 0; i < data.length; i++) {
                char c = (char) (data[i] & 0xFF);
                if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '.' || c == '_' || c == '-') {
                    current.append(c);
                } else {
                    if (current.length() >= 4) {
                        extracted.add(current.toString());
                    }
                    current.setLength(0);
                }
            }

            for (int j = 0; j < extracted.size(); j++) {
                String s = extracted.get(j);
                detectPackageAndSdk(s, info);
            }
        } catch (Exception ignored) {
        }
    }

    private static void detectPackageAndSdk(String str, AabBundleInfo info) {
        if (str == null || str.isEmpty()) return;
        if (str.contains(".") && str.length() > 5 && !str.startsWith("android.") && !str.startsWith("java.") && !str.startsWith("http")) {
            if (str.matches("^[a-z][a-z0-9_]*(\\.[a-z0-9_]+)+$")) {
                if ("com.installed.app".equals(info.getPackageName()) || str.split("\\.").length >= 2) {
                    info.setPackageName(str);
                }
            }
        }
    }

    public static byte[] compileValidBinaryManifest(AabBundleInfo info) {
        String pkg = (info.getPackageName() != null && !info.getPackageName().isEmpty()) ? info.getPackageName() : "com.installed.app";
        int vCode = info.getVersionCode() > 0 ? info.getVersionCode() : 1;
        String vName = (info.getVersionName() != null && !info.getVersionName().isEmpty()) ? info.getVersionName() : "1.0";
        int minSdk = info.getMinSdkVersion() > 0 ? info.getMinSdkVersion() : 21;
        int targetSdk = info.getTargetSdkVersion() > 0 ? info.getTargetSdkVersion() : 34;

        List<String> pool = new ArrayList<String>();
        pool.add("manifest");                               // 0
        pool.add("package");                                // 1
        pool.add(pkg);                                      // 2
        pool.add("http://schemas.android.com/apk/res/android"); // 3
        pool.add("android");                                // 4
        pool.add("versionCode");                            // 5
        pool.add("versionName");                            // 6
        pool.add(vName);                                    // 7
        pool.add("uses-sdk");                               // 8
        pool.add("minSdkVersion");                          // 9
        pool.add("targetSdkVersion");                       // 10
        pool.add("application");                            // 11
        pool.add("label");                                  // 12
        pool.add("allowBackup");                            // 13
        pool.add("activity");                               // 14
        pool.add("name");                                   // 15
        pool.add(pkg + ".MainActivity");                    // 16
        pool.add("exported");                               // 17
        pool.add("intent-filter");                          // 18
        pool.add("action");                                 // 19
        pool.add("android.intent.action.MAIN");             // 20
        pool.add("category");                               // 21
        pool.add("android.intent.category.LAUNCHER");       // 22

        int[] resMap = new int[]{
                0x0101000b, // versionCode
                0x0101000c, // versionName
                0x0101020c, // minSdkVersion
                0x01010270, // targetSdkVersion
                0x01010001, // label
                0x01010280, // allowBackup
                0x01010003, // name
                0x01010010  // exported
        };

        ByteArrayOutputStream treeStream = new ByteArrayOutputStream();
        try {
            writeStartNamespace(treeStream, 4, 3);

            ByteArrayOutputStream manifestAttrs = new ByteArrayOutputStream();
            writeAttribute(manifestAttrs, -1, 1, 2, 0x03, 2);
            writeAttribute(manifestAttrs, 3, 5, -1, 0x10, vCode);
            writeAttribute(manifestAttrs, 3, 6, 7, 0x03, 7);
            writeStartElement(treeStream, -1, 0, manifestAttrs.toByteArray(), 3);

            ByteArrayOutputStream sdkAttrs = new ByteArrayOutputStream();
            writeAttribute(sdkAttrs, 3, 9, -1, 0x10, minSdk);
            writeAttribute(sdkAttrs, 3, 10, -1, 0x10, targetSdk);
            writeStartElement(treeStream, -1, 8, sdkAttrs.toByteArray(), 2);
            writeEndElement(treeStream, -1, 8);

            ByteArrayOutputStream appAttrs = new ByteArrayOutputStream();
            writeAttribute(appAttrs, 3, 12, 0, 0x03, 0);
            writeAttribute(appAttrs, 3, 13, -1, 0x12, 1);
            writeStartElement(treeStream, -1, 11, appAttrs.toByteArray(), 2);

            ByteArrayOutputStream actAttrs = new ByteArrayOutputStream();
            writeAttribute(actAttrs, 3, 15, 16, 0x03, 16);
            writeAttribute(actAttrs, 3, 17, -1, 0x12, 1);
            writeStartElement(treeStream, -1, 14, actAttrs.toByteArray(), 2);

            writeStartElement(treeStream, -1, 18, new byte[0], 0);

            ByteArrayOutputStream actActionAttrs = new ByteArrayOutputStream();
            writeAttribute(actActionAttrs, 3, 15, 20, 0x03, 20);
            writeStartElement(treeStream, -1, 19, actActionAttrs.toByteArray(), 1);
            writeEndElement(treeStream, -1, 19);

            ByteArrayOutputStream actCatAttrs = new ByteArrayOutputStream();
            writeAttribute(actCatAttrs, 3, 15, 22, 0x03, 22);
            writeStartElement(treeStream, -1, 21, actCatAttrs.toByteArray(), 1);
            writeEndElement(treeStream, -1, 21);

            writeEndElement(treeStream, -1, 18); // end intent-filter
            writeEndElement(treeStream, -1, 14); // end activity
            writeEndElement(treeStream, -1, 11); // end application
            writeEndElement(treeStream, -1, 0);  // end manifest

            writeEndNamespace(treeStream, 4, 3);
        } catch (IOException ignored) {
        }

        byte[] stringPoolChunk = buildStringPool(pool);
        byte[] resMapChunk = buildResMapChunk(resMap);
        byte[] xmlTreeBytes = treeStream.toByteArray();

        int totalSize = 8 + stringPoolChunk.length + resMapChunk.length + xmlTreeBytes.length;

        ByteArrayOutputStream finalStream = new ByteArrayOutputStream(totalSize);
        writeShort(finalStream, RES_XML_TYPE);
        writeShort(finalStream, (short) 8);
        writeInt(finalStream, totalSize);

        try {
            finalStream.write(stringPoolChunk);
            finalStream.write(resMapChunk);
            finalStream.write(xmlTreeBytes);
        } catch (IOException ignored) {
        }

        return finalStream.toByteArray();
    }

    private static byte[] buildStringPool(List<String> pool) {
        ByteArrayOutputStream stringBytes = new ByteArrayOutputStream();
        int[] offsets = new int[pool.size()];

        for (int i = 0; i < pool.size(); i++) {
            offsets[i] = stringBytes.size();
            String s = pool.get(i);
            int len = s.length();
            writeShort(stringBytes, (short) len);
            for (int c = 0; c < len; c++) {
                writeShort(stringBytes, (short) s.charAt(c));
            }
            writeShort(stringBytes, (short) 0);
        }

        while (stringBytes.size() % 4 != 0) {
            stringBytes.write(0);
        }

        int headerSize = 28;
        int stringsStart = headerSize + (pool.size() * 4);
        int chunkSize = stringsStart + stringBytes.size();

        ByteArrayOutputStream out = new ByteArrayOutputStream(chunkSize);
        writeShort(out, RES_STRING_POOL_TYPE);
        writeShort(out, (short) headerSize);
        writeInt(out, chunkSize);
        writeInt(out, pool.size());
        writeInt(out, 0);
        writeInt(out, 0);
        writeInt(out, stringsStart);
        writeInt(out, 0);

        for (int j = 0; j < offsets.length; j++) {
            writeInt(out, offsets[j]);
        }

        try {
            out.write(stringBytes.toByteArray());
        } catch (IOException ignored) {
        }

        return out.toByteArray();
    }

    private static byte[] buildResMapChunk(int[] resMap) {
        int chunkSize = 8 + (resMap.length * 4);
        ByteArrayOutputStream out = new ByteArrayOutputStream(chunkSize);
        writeShort(out, RES_XML_RESOURCE_MAP_TYPE);
        writeShort(out, (short) 8);
        writeInt(out, chunkSize);
        for (int i = 0; i < resMap.length; i++) {
            writeInt(out, resMap[i]);
        }
        return out.toByteArray();
    }

    private static void writeStartNamespace(ByteArrayOutputStream out, int prefixIdx, int uriIdx) {
        writeShort(out, RES_XML_START_NAMESPACE_TYPE);
        writeShort(out, (short) 16);
        writeInt(out, 24);
        writeInt(out, 1);
        writeInt(out, -1);
        writeInt(out, prefixIdx);
        writeInt(out, uriIdx);
    }

    private static void writeEndNamespace(ByteArrayOutputStream out, int prefixIdx, int uriIdx) {
        writeShort(out, RES_XML_END_NAMESPACE_TYPE);
        writeShort(out, (short) 16);
        writeInt(out, 24);
        writeInt(out, 1);
        writeInt(out, -1);
        writeInt(out, prefixIdx);
        writeInt(out, uriIdx);
    }

    private static void writeStartElement(ByteArrayOutputStream out, int nsIdx, int nameIdx, byte[] attrs, int attrCount) {
        int chunkSize = 36 + attrs.length;
        writeShort(out, RES_XML_START_ELEMENT_TYPE);
        writeShort(out, (short) 16);
        writeInt(out, chunkSize);
        writeInt(out, 1);
        writeInt(out, -1);
        writeInt(out, nsIdx);
        writeInt(out, nameIdx);
        writeShort(out, (short) 20);
        writeShort(out, (short) 20);
        writeShort(out, (short) attrCount);
        writeShort(out, (short) 0);
        writeShort(out, (short) 0);
        writeShort(out, (short) 0);
        try {
            out.write(attrs);
        } catch (IOException ignored) {
        }
    }

    private static void writeEndElement(ByteArrayOutputStream out, int nsIdx, int nameIdx) {
        writeShort(out, RES_XML_END_ELEMENT_TYPE);
        writeShort(out, (short) 16);
        writeInt(out, 24);
        writeInt(out, 1);
        writeInt(out, -1);
        writeInt(out, nsIdx);
        writeInt(out, nameIdx);
    }

    private static void writeAttribute(ByteArrayOutputStream out, int nsIdx, int nameIdx, int rawValIdx, int type, int data) {
        writeInt(out, nsIdx);
        writeInt(out, nameIdx);
        writeInt(out, rawValIdx);
        writeInt(out, (type << 24) | 0x08);
        writeInt(out, data);
    }

    private static void writeShort(ByteArrayOutputStream out, short val) {
        out.write(val & 0xFF);
        out.write((val >>> 8) & 0xFF);
    }

    private static void writeInt(ByteArrayOutputStream out, int val) {
        out.write(val & 0xFF);
        out.write((val >>> 8) & 0xFF);
        out.write((val >>> 16) & 0xFF);
        out.write((val >>> 24) & 0xFF);
    }

    private static int getInt32(byte[] data, int offset) {
        return (data[offset] & 0xFF)
                | ((data[offset + 1] & 0xFF) << 8)
                | ((data[offset + 2] & 0xFF) << 16)
                | ((data[offset + 3] & 0xFF) << 24);
    }
}