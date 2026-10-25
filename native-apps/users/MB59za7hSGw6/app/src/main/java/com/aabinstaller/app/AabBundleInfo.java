package com.aabinstaller.app;

import java.util.ArrayList;
import java.util.List;

public class AabBundleInfo {
    private String fileName;
    private long fileSize;
    private int dexFileCount;
    private int assetFileCount;
    private int resourceCount;
    private boolean hasManifest;
    private List<String> supportedAbis;
    private List<String> modules;

    public AabBundleInfo() {
        this.fileName = "";
        this.fileSize = 0L;
        this.dexFileCount = 0;
        this.assetFileCount = 0;
        this.resourceCount = 0;
        this.hasManifest = false;
        this.supportedAbis = new ArrayList<String>();
        this.modules = new ArrayList<String>();
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public int getDexFileCount() {
        return dexFileCount;
    }

    public void incrementDexCount() {
        this.dexFileCount++;
    }

    public int getAssetFileCount() {
        return assetFileCount;
    }

    public void incrementAssetCount() {
        this.assetFileCount++;
    }

    public int getResourceCount() {
        return resourceCount;
    }

    public void incrementResourceCount() {
        this.resourceCount++;
    }

    public boolean isHasManifest() {
        return hasManifest;
    }

    public void setHasManifest(boolean hasManifest) {
        this.hasManifest = hasManifest;
    }

    public List<String> getSupportedAbis() {
        return supportedAbis;
    }

    public void addAbi(String abi) {
        if (!supportedAbis.contains(abi)) {
            supportedAbis.add(abi);
        }
    }

    public List<String> getModules() {
        return modules;
    }

    public void addModule(String module) {
        if (!modules.contains(module)) {
            modules.add(module);
        }
    }
}