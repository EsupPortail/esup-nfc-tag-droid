package org.esupportail.esupnfctagdroid.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class RequestPermission {
    private Activity activity;
    private int requestCode;

    public RequestPermission(Activity activity, int requestCode) {
        this.activity = activity;
        this.requestCode = requestCode;
    }

    public int getRequestCode() {
        return requestCode;
    }

    public void requestPermission(@NonNull String[] permissions) {

        if (!needRequestRuntimePermissions()) {
            return;
        }
        requestUnGrantedPermissions(permissions, requestCode);
    }

    private boolean needRequestRuntimePermissions() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    @SuppressLint("NewApi")
    private void requestUnGrantedPermissions(String[] permissions, int requestCode) {
        String[] unGrantedPermissions = findUnGrantedPermissions(permissions);
        if (unGrantedPermissions.length == 0) {
            return;
        }
        activity.requestPermissions(permissions, requestCode);
    }

    @SuppressLint("NewApi")
    private boolean isPermissionGranted(String permission) {
        System.err.print(permission);
        return activity.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    public String[] findUnGrantedPermissions(String[] permissions) {
        List<String> unGrantedPermissionList = new ArrayList<>();
        for (String permission : permissions) {
            if (!isPermissionGranted(permission)) {
                unGrantedPermissionList.add(permission);
            }
        }
        return unGrantedPermissionList.toArray(new String[0]);
    }
}