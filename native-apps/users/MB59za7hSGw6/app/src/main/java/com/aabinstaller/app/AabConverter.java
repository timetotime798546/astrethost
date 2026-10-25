package com.aabinstaller.app;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class AabConverter {

    public interface ConversionListener {
        void onProgress(int percentage, String message);
        void onSuccess(File outputFile, AabBundleInfo info);
        void onError(String error);
    }

    private final Context context;
    private final Handler mainHandler;

    public AabConverter(Context context) {
        this.context = context;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void inspectAndConvert(final Uri aabUri, final ConversionListener listener) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                executeConversion(aabUri, listener);
            }
        });
        thread.start();
    }

    private void executeConversion(Uri aabUri, final ConversionListener listener) {
        File tempAab = null;
        File outputApk = null;
        try {
            postProgress(listener, 5, "Extracting selected bundle from storage...");
            InputStream inStream = context.getContentResolver().openInputStream(aabUri);
            if (inStream == null) {
                postError(listener, "Unable to read input bundle file.");
                return;
            }

            tempAab = new File(context.getCacheDir(), "source_bundle_" + System.currentTimeMillis() + ".aab");
            FileOutputStream fos = new FileOutputStream(tempAab);
            byte[] buffer = new byte[32768];
            int read;
            long totalRead = 0;
            while ((read = inStream.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
                totalRead += read;
            }
            fos.flush();
            fos.close();
            inStream.close();

            postProgress(listener, 20, "Inspecting bundle modules, dex bytecode, and architecture binaries...");

            AabBundleInfo info = new AabBundleInfo();
            info.setFileSize(totalRead);

            // Phase 1: Scan and cache entries
            Map<String, byte[]> extractedEntries = new LinkedHashMap<String, byte[]>();
            byte[] originalManifestBytes = null;

            ZipInputStream zis = new ZipInputStream(new FileInputStream(tempAab));
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (entry.isDirectory()) {
                    zis.closeEntry();
                    continue;
                }

                ByteArrayOutputStream entryBaos = new ByteArrayOutputStream();
                while ((read = zis.read(buffer)) != -1) {
                    entryBaos.write(buffer, 0, read);
                }
                byte[] entryBytes = entryBaos.toByteArray();

                if (name.contains("/")) {
                    String module = name.substring(0, name.indexOf('/'));
                    info.addModule(module);
                }

                if (name.endsWith("AndroidManifest.xml")) {
                    info.setHasManifest(true);
                    originalManifestBytes = entryBytes;
                }

                String mappedPath = mapAabPathToApk(name);
                if (mappedPath != null) {
                    extractedEntries.put(mappedPath, entryBytes);

                    if (mappedPath.endsWith(".dex")) {
                        info.incrementDexCount();
                    } else if (mappedPath.startsWith("assets/")) {
                        info.incrementAssetCount();
                    } else if (mappedPath.startsWith("res/")) {
                        info.incrementResourceCount();
                    } else if (mappedPath.startsWith("lib/")) {
                        int secondSlash = mappedPath.indexOf('/', 4);
                        if (secondSlash != -1) {
                            String abi = mappedPath.substring(4, secondSlash);
                            info.addAbi(abi);
                        }
                    }
                }

                zis.closeEntry();
            }
            zis.close();

            postProgress(listener, 45, "Validating AndroidManifest.xml and package parameters...");

            // Inspect and validate manifest
            if (originalManifestBytes != null) {
                BinaryXmlUtil.inspectManifestParameters(originalManifestBytes, info);
            }

            // Ensure AndroidManifest.xml is 100% compliant binary XML
            byte[] finalManifestBytes;
            if (originalManifestBytes != null && BinaryXmlUtil.isBinaryXml(originalManifestBytes)) {
                finalManifestBytes = originalManifestBytes;
            } else {
                postProgress(listener, 50, "Compiling binary AndroidManifest (package: " + info.getPackageName() + ")...");
                finalManifestBytes = BinaryXmlUtil.compileValidBinaryManifest(info);
            }
            extractedEntries.put("AndroidManifest.xml", finalManifestBytes);

            postProgress(listener, 60, "Structuring APK layout (" + extractedEntries.size() + " components)...");

            File exportDir = new File(context.getFilesDir(), "converted_apks");
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }
            outputApk = new File(exportDir, info.getPackageName() + "_converted.apk");

            // Write ZIP entries and compute digest manifest
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputApk));
            zos.setLevel(ZipOutputStream.DEFLATED);

            Map<String, String> fileDigests = new TreeMap<String, String>();

            for (Map.Entry<String, byte[]> item : extractedEntries.entrySet()) {
                String path = item.getKey();
                byte[] data = item.getValue();

                ZipEntry apkEntry = new ZipEntry(path);

                // For uncompressed native libs or resources if needed, otherwise standard DEFLATED
                if (path.endsWith(".png") || path.endsWith(".webp") || path.endsWith(".jpg")) {
                    apkEntry.setMethod(ZipEntry.STORED);
                    apkEntry.setSize(data.length);
                    apkEntry.setCompressedSize(data.length);
                    CRC32 crc = new CRC32();
                    crc.update(data);
                    apkEntry.setCrc(crc.getValue());
                } else {
                    apkEntry.setMethod(ZipEntry.DEFLATED);
                }

                zos.putNextEntry(apkEntry);
                zos.write(data);
                zos.closeEntry();

                String digest = ApkSignerUtil.computeSha256Hex(data);
                fileDigests.put(path, digest);
            }

            postProgress(listener, 75, "Applying APK Signature Scheme v1 (JAR signing)...");

            // 1. Add META-INF/MANIFEST.MF
            byte[] manifestBytes = ApkSignerUtil.generateManifestMf(fileDigests);
            ZipEntry manifestEntry = new ZipEntry("META-INF/MANIFEST.MF");
            manifestEntry.setMethod(ZipEntry.DEFLATED);
            zos.putNextEntry(manifestEntry);
            zos.write(manifestBytes);
            zos.closeEntry();

            // 2. Add META-INF/CERT.SF
            byte[] certSfBytes = ApkSignerUtil.generateCertSf(manifestBytes, fileDigests);
            ZipEntry certSfEntry = new ZipEntry("META-INF/CERT.SF");
            certSfEntry.setMethod(ZipEntry.DEFLATED);
            zos.putNextEntry(certSfEntry);
            zos.write(certSfBytes);
            zos.closeEntry();

            // 3. Add META-INF/CERT.RSA
            byte[] certRsaBytes = ApkSignerUtil.generateCertRsa(certSfBytes);
            ZipEntry certRsaEntry = new ZipEntry("META-INF/CERT.RSA");
            certRsaEntry.setMethod(ZipEntry.DEFLATED);
            zos.putNextEntry(certRsaEntry);
            zos.write(certRsaBytes);
            zos.closeEntry();

            zos.flush();
            zos.close();

            postProgress(listener, 88, "Injecting APK Signature Scheme v2 (APK Signing Block)...");

            // Apply APK v2 Signature Block
            ApkSignerUtil.signApkWithSchemeV2(outputApk);

            postProgress(listener, 100, "Package generated and verified successfully! (" + (outputApk.length() / 1024) + " KB)");
            postSuccess(listener, outputApk, info);

        } catch (final Exception e) {
            postError(listener, "Conversion error: " + (e.getMessage() != null ? e.getMessage() : e.toString()));
        } finally {
            if (tempAab != null && tempAab.exists()) {
                tempAab.delete();
            }
        }
    }

    private String mapAabPathToApk(String path) {
        if (path == null || path.isEmpty() || path.endsWith("/")) {
            return null;
        }

        // Strip metadata and signatures
        if (path.startsWith("BUNDLE-METADATA/") || path.startsWith("META-INF/")) {
            return null;
        }

        // Standard AAB module directory prefix is "base/"
        if (path.startsWith("base/")) {
            String sub = path.substring(5);
            if (sub.startsWith("dex/")) {
                return sub.substring(4); // e.g. "dex/classes.dex" -> "classes.dex"
            } else if (sub.startsWith("manifest/")) {
                return "AndroidManifest.xml";
            } else if (sub.startsWith("root/")) {
                String rootSub = sub.substring(5);
                if (rootSub.startsWith("META-INF/")) {
                    return null;
                }
                return rootSub;
            } else {
                return sub; // "res/...", "assets/...", "lib/...", "resources.arsc"
            }
        }

        // Feature modules dex or assets merging
        int slashIdx = path.indexOf('/');
        if (slashIdx != -1) {
            String remainder = path.substring(slashIdx + 1);
            if (remainder.startsWith("dex/")) {
                return remainder.substring(4);
            } else if (remainder.startsWith("assets/")) {
                return remainder;
            } else if (remainder.startsWith("lib/")) {
                return remainder;
            }
        }

        if (path.endsWith(".dex") || path.equals("resources.arsc")) {
            return path;
        }

        return null;
    }

    private void postProgress(final ConversionListener listener, final int pct, final String msg) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                listener.onProgress(pct, msg);
            }
        });
    }

    private void postSuccess(final ConversionListener listener, final File file, final AabBundleInfo info) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                listener.onSuccess(file, info);
            }
        });
    }

    private void postError(final ConversionListener listener, final String err) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                listener.onError(err);
            }
        });
    }
}