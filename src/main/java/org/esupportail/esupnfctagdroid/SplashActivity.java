package org.esupportail.esupnfctagdroid;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;

import org.apache.commons.io.IOUtils;
import org.esupportail.esupnfctagdroid.exceptions.NfcTagDroidException;
import org.esupportail.esupnfctagdroid.localstorage.LocalStorage;
import org.esupportail.esupnfctagdroid.requestasync.UrlsHttpRequestAsync;
import org.esupportail.esupnfctagdroid.utils.RequestPermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class SplashActivity extends AppCompatActivity {

    private static Logger log = LoggerFactory.getLogger(SplashActivity.class);
    private static RequestPermission requestPermission;
    private static View view;
    private static Spinner spinner;
    private static Button button;
    private static ImageButton buttonRefresh;
    private static LocalStorage localStorage;
    private static String urlsAddress = "";
    private static String esupNfcTagServerUrl;
    private static Properties esupnfctagProperties = new Properties();
    private static InputStream esupnfctagPropertiesFile = null;
    private static final int time = 5000;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        try {
            esupnfctagPropertiesFile = this.getApplicationContext().getAssets().open("esupnfctag.properties");
            esupnfctagProperties.load(esupnfctagPropertiesFile);
        } catch (IOException e) {
            log.error("Enable to read propertie file");
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        localStorage = LocalStorage.getInstance(this);
        spinner = (Spinner) this.findViewById(R.id.spinner);
        button = (Button) this.findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                launchNfcTag(spinner.getSelectedItem().toString());
            }
        });

        buttonRefresh = (ImageButton) this.findViewById(R.id.button_refresh);
        buttonRefresh.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                refreshSpinner();
            }
        });

        view = (View) findViewById(R.id.splash);
        view.setVisibility(View.INVISIBLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermission = new RequestPermission(this, 0);
            String[] permissions = new String[] { Manifest.permission.READ_PHONE_STATE, Manifest.permission.INTERNET};
            String[] unGrantedPermissions = requestPermission.findUnGrantedPermissions(permissions);
            if (unGrantedPermissions.length == 0) {
                checkParams();
            } else {
                requestPermission.requestPermission(permissions);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermission = new RequestPermission(this, 0);
            String[] permissions = new String[] { Manifest.permission.READ_PHONE_STATE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.INTERNET};
            String[] unGrantedPermissions = requestPermission.findUnGrantedPermissions(permissions);
            if (unGrantedPermissions.length == 0) {
                checkParams();
            } else {
                requestPermission.requestPermission(permissions);
            }
        } else {
            checkParams();
        }
    }

    private void checkParams() {
        boolean splashScreen = Boolean.valueOf(esupnfctagProperties.getProperty("splashScreen"));
        if(!splashScreen) {
            launchNfcTag(esupnfctagProperties.getProperty("esupNfcTagServerUrl"));
        } else {
            String numeroId = localStorage.getValue("numeroId");
            String esupNfcTagUrl = localStorage.getValue("esupNfcTagUrl");
            if(esupNfcTagUrl != null && !esupNfcTagUrl.equals("") && numeroId != null && !numeroId.equals("")) {
                launchNfcTag(esupNfcTagUrl);
            } else {
                view.setVisibility(View.VISIBLE);
                urlsAddress = esupnfctagProperties.getProperty("urlsAddress");
                refreshSpinner();
            }
        }
    }

    private void refreshSpinner() {
        String urls = null;
        if(urlsAddress != null && !urlsAddress.equals("")) {
            try {
                UrlsHttpRequestAsync urlsTask = new UrlsHttpRequestAsync();
                String webUrls = urlsTask.execute(urlsAddress).get(time, TimeUnit.MILLISECONDS);
                if (webUrls != null) {
                    urls = webUrls;
                    localStorage.updateValue("urls", urls);
                    log.info("web URLS " + urls);
                }
            } catch (InterruptedException e) {
                log.error("error webUrls task", e);
                throw new NfcTagDroidException("InterruptedException", e);
            } catch (ExecutionException e) {
                log.error("error webUrls task", e);
                throw new NfcTagDroidException("InterruptedException", e);
            } catch (TimeoutException e) {
                log.warn("Time out");
            }
        }
        if(urls == null) {
            String localUrls = localStorage.getValue("urls");
            if (localUrls != null && !localUrls.equals("")) {
                urls = localUrls;
                log.info("local URLS " + urls);
            } else {
                InputStream fileUrls = null;
                try {
                    fileUrls = this.getApplicationContext().getAssets().open("urls");
                    urls = IOUtils.toString(fileUrls, "UTF8");
                    localStorage.updateValue("urls", urls);
                    log.info("file URLS " + urls);
                } catch (IOException e) {
                    log.error("enable to read urls file");
                }
            }
        }
        String[] arraySpinner = urls.split("\n");
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, arraySpinner);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void launchNfcTag(String url) {
        Intent intent = new Intent(SplashActivity.this, NfcTagDroidActivity.class);
        intent.putExtra("url", url);
        startActivity(intent);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == requestPermission.getRequestCode()) {
            if (grantResults.length > 0) {
                for (int grantResult : grantResults) {
                    if (grantResult != PERMISSION_GRANTED) {
                        finish();
                        return;
                    }
                }
                checkParams();
            } else {
                finish();
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.exit)
                    .setMessage(R.string.msg_exit)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                            Process.killProcess(Process.myPid());
                            System.exit(1);
                        }
                    })
                    .setNegativeButton(R.string.no, null)
                    .show();
        }
        return true;
    }
}
