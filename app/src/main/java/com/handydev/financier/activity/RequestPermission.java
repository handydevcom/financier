package com.handydev.financier.activity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;

import androidx.core.content.ContextCompat;

import static android.os.Build.VERSION.SDK_INT;

public class RequestPermission {

    public static boolean isRequestingPermission(Context context, String permission) {
        if (!checkPermission(context, permission)) {
            RequestPermissionActivity_.intent(context).requestedPermission(permission).start();
            return true;
        }
        return false;
    }

    public static boolean checkPermission(Context ctx, String permission) {
        if(SDK_INT >= 30) {
            if(permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                return Environment.isExternalStorageManager();
            }
        }
        return ContextCompat.checkSelfPermission(ctx, permission) == PackageManager.PERMISSION_GRANTED;
    }

    static boolean isRequestingPermissions(Context context, String... permissions) {
        for (String permission : permissions) {
            if (isRequestingPermission(context, permission)) return true;
        }
        return false;
    }

}
