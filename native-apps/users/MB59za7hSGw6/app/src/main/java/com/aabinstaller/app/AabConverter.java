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
import java.util.Map;
import java.util.TreeMap;
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
            postProgress(listener, 5, "Reading selected file from storage...");
            InputStream inStream = context.getContentResolver().openInputStream(aabUri);
            if (inStream == null) {
                postError(listener, "Failed to open input stream for selected file.");
                return;
            }

            tempAab = new File(context.getCacheDir(), "source_input.bundle");
            FileOutputStream fos = new FileOutputStream(tempAab);
            byte[] buffer = new byte[16384];
            int read;
            long totalRead = 0;
            while ((read = inStream.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
                totalRead += read;
            }
            fos.flush();
            fos.close();
            inStream.close();

            postProgress(listener, 25, "Analyzing bundle structure and metadata (" + (totalRead / 1024) + " KB)...");

            AabBundleInfo info = inspectBundle(tempAab);
            info.setFileSize(totalRead);

            postProgress(listener, 45, "Found " + info.getDexFileCount() + " Dex files and " + info.getSupportedAbis().size() + " ABI architectures.");

            // Output APK file
            File exportDir = new File(context.getFilesDir(), "converted_apks");
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }
            outputApk = new File(exportDir, "app_from_aab_" + System.currentTimeMillis() + ".apk");

            postProgress(listener, 55, "Remapping bundle directories (base/dex, base/res, base/assets, base/lib)...");

            ZipInputStream zis = new ZipInputStream(new FileInputStream(tempAab));
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputApk));
            zos.setLevel(ZipOutputStream.DEFLATED);

            ZipEntry entry;
            Map<String, String> fileDigests = new TreeMap<String, String>();
            int processedCount = 0;

            while ((entry = zis.getNextEntry()) != null) {
                String originalName = entry.getName();
                String targetName = mapAabPathToApk(originalName);

                if (targetName != null && !targetName.startsWith("META-INF/")) {
                    ByteArrayOutputStream entryBaos = new ByteArrayOutputStream();
                    while ((read = zis.read(buffer)) != -1) {
                        entryBaos.write(buffer, 0, read);
                    }
                    byte[] entryBytes = entryBaos.toByteArray();

                    ZipEntry newEntry = new ZipEntry(targetName);
                    zos.putNextEntry(newEntry);
                    zos.write(entryBytes);
                    zos.closeEntry();

                    String digest = ApkSignerUtil.computeSha256(entryBytes);
                    fileDigests.put(targetName, digest);
                    processedCount++;
                }
                zis.closeEntry();
            }
            zis.close();

            postProgress(listener, 80, "Repackaged " + processedCount + " entries. Generating package signature...");

            // Add META-INF/MANIFEST.MF
            byte[] manifestBytes = ApkSignerUtil.generateManifestMf(fileDigests);
            ZipEntry manifestEntry = new ZipEntry("META-INF/MANIFEST.MF");
            zos.putNextEntry(manifestEntry);
            zos.write(manifestBytes);
            zos.closeEntry();

            // Add META-INF/CERT.SF
            byte[] certSfBytes = ApkSignerUtil.generateCertSf(manifestBytes, fileDigests);
            ZipEntry certSfEntry = new ZipEntry("META-INF/CERT.SF");
            zos.putNextEntry(certSfEntry);
            zos.write(certSfBytes);
            zos.closeEntry();

            // Add META-INF/CERT.RSA
            byte[] certRsaBytes = ApkSignerUtil.generateTestCertRsa();
            ZipEntry certRsaEntry = new ZipEntry("META-INF/CERT.RSA");
            zos.putNextEntry(certRsaEntry);
            zos.write(certRsaBytes);
            zos.closeEntry();

            zos.flush();
            zos.close();

            postProgress(listener, 100, "Conversion complete! APK generated successfully: " + (outputApk.length() / 1024) + " KB");
            postSuccess(listener, outputApk, info);

        } catch (final Exception e) {
            postError(listener, "Error during conversion: " + e.getMessage());
        } finally {
            if (tempAab != null && tempAab.exists()) {
                tempAab.delete();
            }
        }
    }

    private AabBundleInfo inspectBundle(File bundleFile) {
        AabBundleInfo info = new AabBundleInfo();
        try {
            ZipInputStream zis = new ZipInputStream(new FileInputStream(bundleFile));
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (name.contains("/")) {
                    String module = name.substring(0, name.indexOf('/'));
                    info.addModule(module);
                }

                if (name.endsWith(".dex")) {
                    info.incrementDexCount();
                } else if (name.contains("/assets/")) {
                    info.incrementAssetCount();
                } else if (name.contains("/res/")) {
                    info.incrementResourceCount();
                } else if (name.endsWith("AndroidManifest.xml")) {
                    info.setHasManifest(true);
                }

                if (name.contains("/lib/")) {
                    int libIndex = name.indexOf("/lib/") + 5;
                    int slashAfter = name.indexOf('/', libIndex);
                    if (slashAfter != -1) {
                        String abi = name.substring(libIndex, slashAfter);
                        info.addAbi(abi);
                    }
                }
                zis.closeEntry();
            }
            zis.close();
        } catch (Exception ignored) {
        }
        return info;
    }

    private String mapAabPathToApk(String path) {
        if (path == null || path.isEmpty() || path.endsWith("/")) {
            return null;
        }

        // Ignore metadata not needed in APK
        if (path.startsWith("BUNDLE-METADATA/") || path.startsWith("base/root/META-INF/")) {
            return null;
        }

        // Standard AAB module is typically "base/"
        if (path.startsWith("base/")) {
            String sub = path.substring(5); // strip "base/"
            if (sub.startsWith("dex/")) {
                return sub.substring(4); // "dex/classes.dex" -> "classes.dex"
            } else if (sub.startsWith("manifest/")) {
                return sub.substring(9); // "manifest/AndroidManifest.xml" -> "AndroidManifest.xml"
            } else {
                return sub; // "res/...", "assets/...", "lib/...", "resources.arsc"
            }
        }

        // If user provided an .apks or pre-structured package
        if (path.endsWith(".apk")) {
            return path;
        }

        // Feature modules or root files
        if (path.startsWith("root/")) {
            return path.substring(5);
        }

        return path;
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