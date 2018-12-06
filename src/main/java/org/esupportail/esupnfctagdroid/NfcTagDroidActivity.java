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
package org.esupportail.esupnfctagdroid;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Build;
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

import org.esupportail.esupnfctagdroid.authentication.CsnNfcProvider;
import org.esupportail.esupnfctagdroid.authentication.DesfireNfcProvider;
import org.esupportail.esupnfctagdroid.beans.NfcResultBean;
import org.esupportail.esupnfctagdroid.exceptions.ExceptionHandler;
import org.esupportail.esupnfctagdroid.exceptions.NfcTagDroidException;
import org.esupportail.esupnfctagdroid.exceptions.NfcTagDroidInvalidTagException;
import org.esupportail.esupnfctagdroid.exceptions.NfcTagDroidPleaseRetryTagException;
import org.esupportail.esupnfctagdroid.localstorage.LocalStorage;
import org.esupportail.esupnfctagdroid.localstorage.LocalStorageJavaScriptInterface;
import org.esupportail.esupnfctagdroid.utils.AndroidJavaScriptInterface;
import org.esupportail.esupnfctagdroid.utils.ToastThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class NfcTagDroidActivity extends Activity implements NfcAdapter.ReaderCallback {

    private NfcAdapter mAdapter;
    public static String ESUP_NFC_TAG_SERVER_URL = null;
    public static String NFC_TYPE;
    public static LocalStorage localStorage;
    private static final Logger log = LoggerFactory.getLogger(NfcTagDroidActivity.class);
    private static WebView view;
    private static String url;
    private static ProgressBar progressBar = null;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        localStorage = LocalStorage.getInstance(this);
        ESUP_NFC_TAG_SERVER_URL = localStorage.getValue("esupnfctagurl");
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String urlExtra = extras.getString("url");
            if (urlExtra != null && !urlExtra.equals("")) {
                ESUP_NFC_TAG_SERVER_URL = urlExtra;
                localStorage.updateValue("esupnfctagurl", ESUP_NFC_TAG_SERVER_URL);
            }
        }
        log.info("connecting to " + ESUP_NFC_TAG_SERVER_URL);
        CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(getApplicationContext()));
        setContentView(R.layout.activity_main);
        mAdapter = NfcAdapter.getDefaultAdapter(this);
        view = (WebView) findViewById(R.id.webView);
        checkHardware(mAdapter);
        String numeroId = localStorage.getValue("numeroId");
        String imei = "EsupNfcTagDroid";
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        imei = telephonyManager.getDeviceId();
        url = ESUP_NFC_TAG_SERVER_URL + "/nfc-index?numeroId=" + numeroId + "&imei=" + imei + "&macAddress=" + getMacAddr();
        progressBar = (ProgressBar) findViewById(R.id.loadingPanel);
        view.clearCache(true);
        view.addJavascriptInterface(new LocalStorageJavaScriptInterface(this), "AndroidLocalStorage");
        view.addJavascriptInterface(new AndroidJavaScriptInterface(this), "Android");
        view.getSettings().setSaveFormData(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            view.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        }
        view.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onProgressChanged(WebView view, int progress) {
                if (progress == 100) {
                    NFC_TYPE = localStorage.getValue("authType");
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
        checkHardware(mAdapter);
        mAdapter.enableReaderMode(this, this, NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS, null);
    }

    @Override
    public void onTagDiscovered(final Tag tag) {
        if (localStorage.getValue("readyToScan").equals("ok")) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressBar.setVisibility(View.VISIBLE);
                }
            });

            localStorage.updateValue("readyToScan", "ko");
            synchronized (tag) {
                String toastText = "";
                int rStatus = R.raw.fail;

                try {
                    NfcResultBean nfcResult = read(tag);
                    if (NfcResultBean.CODE.ERROR.equals(nfcResult.getCode())) {
                        log.warn(getString(R.string.log_msg_tag_ko));
                        if (nfcResult.getMsg() == null || nfcResult.getMsg() == "") {
                            toastText = getString(R.string.log_msg_tag_ko);
                            runOnUiThread(ToastThread.getInstance(getApplicationContext(), toastText));
                        }
                        localStorage.updateValue("readyToScan", "ok");
                    } else {

                        rStatus = R.raw.success;
                        if (nfcResult.getFullApdu() != null) {
                            log.info(getString(R.string.log_msg_tag_ok) + " : " + nfcResult.getFullApdu().replace("\n", " "));
                        } else {
                            log.info(getString(R.string.log_msg_tag_ok) + " : " + nfcResult.getMsg());
                        }
                    }
                } catch (NfcTagDroidInvalidTagException e) {
                    log.info(getString(R.string.log_msg_invalid_auth), e);
                    toastText = getString(R.string.msg_tag_ko);
                    runOnUiThread(ToastThread.getInstance(getApplicationContext(), toastText));
                    localStorage.updateValue("readyToScan", "ok");
                } catch (NfcTagDroidPleaseRetryTagException e) {
                    log.warn(getString(R.string.log_msg_retry_auth), e);
                    toastText = getString(R.string.msg_retry);
                    runOnUiThread(ToastThread.getInstance(getApplicationContext(), toastText));
                    localStorage.updateValue("readyToScan", "ok");
                } catch (Exception e) {
                    log.error(getString(R.string.log_msg_unknow_err), e);
                    toastText = getString(R.string.msg_unknow_err);
                    runOnUiThread(ToastThread.getInstance(getApplicationContext(), toastText));
                    localStorage.updateValue("readyToScan", "ok");

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
            log.warn("onTagDiscovered but localStorage.getValue(\"readyToScan\") = " + localStorage.getValue("readyToScan"));
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

    public void launchSplash() {
        Intent intent = new Intent(NfcTagDroidActivity.this, SplashActivity.class);
        startActivity(intent);
        finish();
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
