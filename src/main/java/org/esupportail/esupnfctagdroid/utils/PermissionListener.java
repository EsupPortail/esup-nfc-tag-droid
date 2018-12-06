package org.esupportail.esupnfctagdroid.utils;

import android.app.Activity;

public class PermissionListener implements RequestPermissionHandler.RequestPermissionListener {

    Activity activity;

    public PermissionListener(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void onSuccess() {

    }

    @Override
    public void onFailed() {
        activity.runOnUiThread(ToastThread.getInstance(activity.getApplicationContext(), "request permission failed"));
        activity.finish();
    }
}
