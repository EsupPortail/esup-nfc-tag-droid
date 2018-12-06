package org.esupportail.esupnfctagdroid;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
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
import org.esupportail.esupnfctagdroid.utils.PermissionListener;
import org.esupportail.esupnfctagdroid.utils.RequestPermissionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SplashActivity extends AppCompatActivity {

    private static Logger log;
    private RequestPermissionHandler mRequestPermissionHandler = new RequestPermissionHandler();
    private static Spinner spinner;
    private static Button button;
    private static ImageButton buttonRefresh;
    private static LocalStorage localStorage;
    private static String urlsAddress = "";
    private static final int time = 5000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        String[] permissions = new String[] { Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_EXTERNAL_STORAGE };
        mRequestPermissionHandler.requestPermission(this, permissions, 1, new PermissionListener(this));
        log = LoggerFactory.getLogger(SplashActivity.class);
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
                try {
                    refreshSpinner();
                } catch (IOException e) {
                    throw new NfcTagDroidException("can't get property from properties files !", e);
                }
            }
        });

        boolean splashScreen = false;
        String esupNfcTagServerUrl = localStorage.getValue("esupnfctagurl");
        String numeroId = localStorage.getValue("numeroId");
        InputStream esupnfctagPropertiesFile = null;
        try {
            esupnfctagPropertiesFile = this.getApplicationContext().getAssets().open("esupnfctag.properties");
            Properties esupnfctagProperties = new Properties();
            esupnfctagProperties.load(esupnfctagPropertiesFile);
            splashScreen = Boolean.valueOf(esupnfctagProperties.getProperty("splashScreen"));
            urlsAddress = esupnfctagProperties.getProperty("urlsAddress");
            if(!splashScreen) {
                esupNfcTagServerUrl = esupnfctagProperties.getProperty("esupNfcTagServerUrl");
                launchNfcTag(esupNfcTagServerUrl);
            } else {
                if(esupNfcTagServerUrl != null && !esupNfcTagServerUrl.equals("") && numeroId != null && !numeroId.equals("")) {
                    launchNfcTag(esupNfcTagServerUrl);
                } else {
                    refreshSpinner();
                }
            }
        } catch (IOException e) {
            throw new NfcTagDroidException("can't get property from properties files !", e);
        }
    }

    private void refreshSpinner() throws IOException {
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
                InputStream fileUrls = this.getApplicationContext().getAssets().open("urls");
                urls = IOUtils.toString(fileUrls, "UTF8");
                localStorage.updateValue("urls", urls);
                log.info("file URLS " + urls);
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mRequestPermissionHandler.onRequestPermissionsResult(requestCode, permissions,
                grantResults);
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
