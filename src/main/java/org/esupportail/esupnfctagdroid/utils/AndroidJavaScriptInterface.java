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
package org.esupportail.esupnfctagdroid.utils;

import android.database.sqlite.SQLiteDatabase;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;

import org.esupportail.esupnfctagdroid.NfcTagDroidActivity;
import org.esupportail.esupnfctagdroid.localstorage.LocalStorage;

public class AndroidJavaScriptInterface {
    private NfcTagDroidActivity activity;
    private SQLiteDatabase database;
    private LocalStorage localStorageDBHelper;

    public AndroidJavaScriptInterface(NfcTagDroidActivity activity) {
        this.activity = activity;
        localStorageDBHelper = LocalStorage.getInstance(activity);
    }
    @JavascriptInterface
    public void disconnect(){
        database = localStorageDBHelper.getWritableDatabase();
        database.delete(LocalStorage.LOCALSTORAGE_TABLE_NAME, null, null);
        CookieManager.getInstance().removeAllCookies(null);
        activity.launchSplash();
    }
}
