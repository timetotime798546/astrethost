package com.aabinstaller.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class PackageInstallManager {

    public static final int REQ_MANAGE_UNKNOWN_APPS = 1001;

    public static boolean checkInstallPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return activity.getPackageManager().canRequestPackageInstalls();
        }
        return true;
    }

    public static void requestInstallPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                intent.setData(Uri.parse("package:" + activity.getPackageName()));
                activity.startActivityForResult(intent, REQ_MANAGE_UNKNOWN_APPS);
            } catch (Exception e) {
                Intent intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
                activity.startActivity(intent);
            }
        }
    }

    public static void installApk(Context context, File apkFile) {
        if (apkFile == null || !apkFile.exists()) {
            Toast.makeText(context, "APK file does not exist.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Uri apkUri = Uri.parse("content://" + ApkFileProvider.AUTHORITY + "/" + apkFile.getName());
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "Starting PackageInstaller session fallback...", Toast.LENGTH_SHORT).show();
            installViaPackageInstallerSession(context, apkFile);
        }
    }

    public static void installViaPackageInstallerSession(Context context, File apkFile) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Toast.makeText(context, "OS does not support PackageInstaller sessions", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();
            PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                    PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            params.setSize(apkFile.length());

            int sessionId = packageInstaller.createSession(params);
            PackageInstaller.Session session = packageInstaller.openSession(sessionId);

            OutputStream out = session.openWrite("package_install_" + System.currentTimeMillis(), 0, apkFile.length());
            InputStream in = new FileInputStream(apkFile);
            byte[] buf = new byte[32768];
            int c;
            while ((c = in.read(buf)) != -1) {
                out.write(buf, 0, c);
            }
            session.fsync(out);
            in.close();
            out.close();

            Intent intent = new Intent(context, MainActivity.class);
            intent.setAction("com.aabinstaller.app.INSTALL_COMPLETE");
            android.app.PendingIntent pendingIntent;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                pendingIntent = android.app.PendingIntent.getActivity(
                        context, 0, intent, android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);
            } else {
                pendingIntent = android.app.PendingIntent.getActivity(
                        context, 0, intent, android.app.PendingIntent.FLAG_UPDATE_CURRENT);
            }

            session.commit(pendingIntent.getIntentSender());
            session.close();

            Toast.makeText(context, "Installation session submitted.", Toast.LENGTH_SHORT).show();
        } catch (Exception ex) {
            Toast.makeText(context, "Session install error: " + ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}