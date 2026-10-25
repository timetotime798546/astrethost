package com.aabinstaller.app;

import java.util.ArrayList;
import java.util.List;

public class AabBundleInfo {
    private String fileName;
    private long fileSize;
    private String packageName;
    private int versionCode;
    private String versionName;
    private int minSdkVersion;
    private int targetSdkVersion;
    private int dexFileCount;
    private int assetFileCount;
    private int resourceCount;
    private boolean hasManifest;
    private boolean isManifestBinary;
    private List<String> supportedAbis;
    private List<String> modules;

    public AabBundleInfo() {
        this.fileName = "";
        this.fileSize = 0L;
        this.packageName = "com.installed.app";
        this.versionCode = 1;
        this.versionName = "1.0";
        this.minSdkVersion = 21;
        this.targetSdkVersion = 34;
        this.dexFileCount = 0;
        this.assetFileCount = 0;
        this.resourceCount = 0;
        this.hasManifest = false;
        this.isManifestBinary = false;
        this.supportedAbis = new ArrayList<String>();
        this.modules = new ArrayList<String>();
    }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }

    public int getVersionCode() { return versionCode; }
    public void setVersionCode(int versionCode) { this.versionCode = versionCode; }

    public String getVersionName() { return versionName; }
    public void setVersionName(String versionName) { this.versionName = versionName; }

    public int getMinSdkVersion() { return minSdkVersion; }
    public void setMinSdkVersion(int minSdkVersion) { this.minSdkVersion = minSdkVersion; }

    public int getTargetSdkVersion() { return targetSdkVersion; }
    public void setTargetSdkVersion(int targetSdkVersion) { this.targetSdkVersion = targetSdkVersion; }

    public int getDexFileCount() { return dexFileCount; }
    public void incrementDexCount() { this.dexFileCount++; }

    public int getAssetFileCount() { return assetFileCount; }
    public void incrementAssetCount() { this.assetFileCount++; }

    public int getResourceCount() { return resourceCount; }
    public void incrementResourceCount() { this.resourceCount++; }

    public boolean isHasManifest() { return hasManifest; }
    public void setHasManifest(boolean hasManifest) { this.hasManifest = hasManifest; }

    public boolean isManifestBinary() { return isManifestBinary; }
    public void setManifestBinary(boolean manifestBinary) { isManifestBinary = manifestBinary; }

    public List<String> getSupportedAbis() { return supportedAbis; }
    public void addAbi(String abi) {
        if (!supportedAbis.contains(abi)) {
            supportedAbis.add(abi);
        }
    }

    public List<String> getModules() { return modules; }
    public void addModule(String module) {
        if (!modules.contains(module)) {
            modules.add(module);
        }
    }
}