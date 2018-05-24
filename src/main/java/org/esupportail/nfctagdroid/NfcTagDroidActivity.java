/**
 * Licensed to ESUP-Portail under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 *
 * ESUP-Portail licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.esupportail.nfctagdroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;

import android.nfc.NfcAdapter;
import android.nfc.Tag;

import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.esupportail.nfctagdroid.exceptions.NfcTagDroidException;
import org.esupportail.nfctagdroid.exceptions.NfcTagDroidInvalidTagException;
import org.esupportail.nfctagdroid.exceptions.NfcTagDroidPleaseRetryTagException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.esupportail.nfctagdroid.authentication.CsnNfcProvider;
import org.esupportail.nfctagdroid.authentication.DesfireNfcProvider;
import org.esupportail.nfctagdroid.beans.NfcResultBean;
import org.esupportail.nfctagdroid.localstorage.LocalStorageJavaScriptInterface;

import java.io.IOException;
import java.io.InputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.esupportail.nfctagdroid.exceptions.ExceptionHandler;
import org.esupportail.nfctagdroid.localstorage.LocalStorage;

public class NfcTagDroidActivity extends Activity implements NfcAdapter.ReaderCallback{

    private NfcAdapter mAdapter;
    public static String ESUP_NFC_TAG_SERVER_URL = null;
    public static String NFC_TYPE;
    public static LocalStorage localStorageDBHelper;
    private static final Logger log = LoggerFactory.getLogger(NfcTagDroidActivity.class);
    private static WebView view;
    private static String url;
    private static ProgressBar progressBar = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ESUP_NFC_TAG_SERVER_URL = getEsupNfcTagServerUrl(getApplicationContext());
        //To keep session for desfire async requests
        CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
        LocalStorage.getInstance(getApplicationContext());
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(getApplicationContext()));
        setContentView(R.layout.activity_main);
        mAdapter = NfcAdapter.getDefaultAdapter(this);
        checkHardware(mAdapter);
        localStorageDBHelper = LocalStorage.getInstance(this.getApplicationContext());
        String numeroId = localStorageDBHelper.getValue("numeroId");
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String imei = telephonyManager.getDeviceId();
        url = ESUP_NFC_TAG_SERVER_URL + "/nfc-index?numeroId=" + numeroId + "&imei=" + imei + "&macAddress=" + getMacAddr() + "&apkVersion=" + getApkVersion();
        view = (WebView) this.findViewById(R.id.webView);
        progressBar = (ProgressBar) findViewById(R.id.loadingPanel);
        //progressBar.setVisibility(View.INVISIBLE);
        view.clearCache(true);
        view.addJavascriptInterface(new LocalStorageJavaScriptInterface(this.getApplicationContext()), "AndroidLocalStorage");
        view.addJavascriptInterface(new AndroidJavaScriptInterface(this.getApplicationContext()), "Android");

        view.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onProgressChanged(WebView view, int progress) {
                if (progress == 100) {
                    NFC_TYPE = localStorageDBHelper.getValue("authType");
                }
            }

            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                log.info("Webview console message : " + consoleMessage.message());
                return false;
            }

        });

        view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                view.reload();
                findViewById(R.id.loadingPanel).setVisibility(View.INVISIBLE);
                return true;
            }
        });
        view.getSettings().setAllowContentAccess(true);
        WebSettings webSettings = view.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setDatabasePath(this.getFilesDir().getParentFile().getPath() + "/databases/");

        view.setDownloadListener(new DownloadListener() {
            public void onDownloadStart(String url, String userAgent,
                                        String contentDisposition, String mimetype,
                                        long contentLength) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            }

        });

        view.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
            }
        });

        view.loadUrl(url);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    @Override
    protected void onPause() {
        super.onPause();
        log.info("Enter pause mode");
        view.loadUrl("about:blank");
        view.onPause();
        view.pauseTimers();
        mAdapter.disableReaderMode(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        log.info("App is resume");
        view.onResume();
        view.loadUrl(url);
        view.resumeTimers();
        ESUP_NFC_TAG_SERVER_URL = getEsupNfcTagServerUrl(getApplicationContext());
        checkHardware(mAdapter);
        mAdapter.enableReaderMode(this, this, NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS, null);
    }

    @Override
    public void onTagDiscovered(final Tag tag) {
        if (localStorageDBHelper.getValue("readyToScan").equals("ok")) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressBar.setVisibility(View.VISIBLE);
                }
            });

            localStorageDBHelper.updateValue("readyToScan", "ko");
            synchronized (tag) {
                String toastText = "";
                int rStatus = R.raw.fail;

                try {
                    NfcResultBean nfcResult = read(tag);
                    if (NfcResultBean.CODE.ERROR.equals(nfcResult.getCode())) {
                        log.warn(getString(R.string.log_msg_tag_ko));
                        toastText = nfcResult.getMsg();
                        runOnUiThread(ToastThread.getInstance(getApplicationContext(), rStatus, toastText));
                        localStorageDBHelper.updateValue("readyToScan", "ok");
                    } else{
                        rStatus = R.raw.success;
                        log.info(getString(R.string.log_msg_tag_ok) + " : " + nfcResult.getFullApdu().replace("\n", " "));
                    }
                } catch (NfcTagDroidInvalidTagException e) {
                    log.info(getString(R.string.log_msg_invalid_auth), e);
                    toastText = getString(R.string.msg_tag_ko);
                    runOnUiThread(ToastThread.getInstance(getApplicationContext(), rStatus, toastText));
                    localStorageDBHelper.updateValue("readyToScan", "ok");
                } catch (NfcTagDroidPleaseRetryTagException e) {
                    log.warn(getString(R.string.log_msg_retry_auth), e);
                    toastText = getString(R.string.msg_retry);
                    runOnUiThread(ToastThread.getInstance(getApplicationContext(), rStatus, toastText));
                    localStorageDBHelper.updateValue("readyToScan", "ok");
                } catch (Exception e) {
                    log.error(getString(R.string.log_msg_unknow_err), e);
                    toastText = getString(R.string.msg_unknow_err);
                    runOnUiThread(ToastThread.getInstance(getApplicationContext(), rStatus, toastText));
                    localStorageDBHelper.updateValue("readyToScan", "ok");

                }
                MediaPlayer mp = MediaPlayer.create(getApplicationContext(), rStatus);
                mp.start();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.INVISIBLE);
                    }
                });
            }
        } else {
            log.warn("onTagDiscovered but localStorageDBHelper.getValue(\"readyToScan\") = " + localStorageDBHelper.getValue("readyToScan"));
        }

    }

    protected NfcResultBean read(Tag tag) throws ExecutionException, InterruptedException {
        NfcResultBean nfcResult = null;
        if (tag != null) {
            if (NFC_TYPE.equals("DESFIRE")) {
                DesfireNfcProvider desfireNfcProvider = new DesfireNfcProvider();
                nfcResult = desfireNfcProvider.desfireRead(tag);
            } else if (NFC_TYPE.equals("CSN")) {
                CsnNfcProvider csnNfcProvider = new CsnNfcProvider();
                nfcResult = csnNfcProvider.csnRead(tag);
            }
        }
        return nfcResult;
    }

    public boolean checkHardware(NfcAdapter mAdapter) {
        if (mAdapter == null) {
            Toast.makeText(this, getString(R.string.msg_error_nfc), Toast.LENGTH_LONG).show();
            finish();
            return false;
        }

        if (!mAdapter.isEnabled()) {
            Toast.makeText(this, getString(R.string.msg_activ_nfc), Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
        }

        if (!isOnline()) {
            view.loadUrl("about:blank");
            Toast.makeText(this, getString(R.string.msg_activ_network), Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
        }
        return true;
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

    private String getApkVersion() {
        String version = "unknow";
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            throw new NfcTagDroidException(getString(R.string.msg_getApkVersion_err), e);
        }
        return version;
    }

    private static String getEsupNfcTagServerUrl(Context context) {
        try {
            InputStream esupnfctagPropertiesFile = context.getAssets().open("esupnfctag.properties");
            Properties props = new Properties();
            props.load(esupnfctagPropertiesFile);
            String esupNfcTagServerUrl = props.getProperty("esupNfcTagServerUrl");
            return esupNfcTagServerUrl;
        } catch (IOException e) {
            throw new NfcTagDroidException("can't get esupNfcTagServerUrl property from esupnfctag.properties file !", e);
        }
    }

    public static String getMacAddr() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(Integer.toHexString(b & 0xFF) + ":");
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString();
            }
        } catch (Exception e) {
            throw new NfcTagDroidException("can't get mac address", e);
        }
        return "";
    }
}

