package com.offlinecctvrecord.app;

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class StorageHelper {

    public static File getCctvDirectory(Context context) {
        File dir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        if (dir == null) {
            dir = new File(context.getFilesDir(), "CCTV_Recordings");
        } else {
            dir = new File(dir, "CCTV_Recordings");
        }
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static List<RecordingModel> getSavedRecordings(Context context) {
        List<RecordingModel> list = new ArrayList<RecordingModel>();
        File cctvDir = getCctvDirectory(context);
        File[] files = cctvDir.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                File f = files[i];
                if (f.isFile() && f.getName().toLowerCase().endsWith(".mp4")) {
                    list.add(new RecordingModel(f));
                }
            }
        }
        // Sort descending by modified date
        Collections.sort(list, new Comparator<RecordingModel>() {
            @Override
            public int compare(RecordingModel o1, RecordingModel o2) {
                if (o2.getLastModified() > o1.getLastModified()) {
                    return 1;
                } else if (o2.getLastModified() < o1.getLastModified()) {
                    return -1;
                }
                return 0;
            }
        });
        return list;
    }

    public static long getTotalCctvBytes(Context context) {
        long total = 0;
        File cctvDir = getCctvDirectory(context);
        File[] files = cctvDir.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                File f = files[i];
                if (f.isFile()) {
                    total += f.length();
                }
            }
        }
        return total;
    }

    public static long getAvailableInternalDiskSpace(Context context) {
        try {
            File path = getCctvDirectory(context);
            StatFs stat = new StatFs(path.getPath());
            long availableBlocks = stat.getAvailableBlocksLong();
            long blockSize = stat.getBlockSizeLong();
            return availableBlocks * blockSize;
        } catch (Exception e) {
            return 0;
        }
    }

    public static String formatBytes(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        if (digitGroups >= units.length) digitGroups = units.length - 1;
        DecimalFormat df = new DecimalFormat("#,##0.#");
        return df.format(bytes / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}