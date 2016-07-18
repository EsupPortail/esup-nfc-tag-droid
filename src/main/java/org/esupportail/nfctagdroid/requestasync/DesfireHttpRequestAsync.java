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
package org.esupportail.nfctagdroid.requestasync;

import android.os.AsyncTask;

import org.esupportail.nfctagdroid.exceptions.NfcTagDroidException;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.esupportail.nfctagdroid.NfcTacDroidActivity;
import org.esupportail.nfctagdroid.localstorage.LocalStorage;

public class DesfireHttpRequestAsync extends AsyncTask<String, String, String> {

    protected String doInBackground(String... params)  {

        try {
            String numeroId = LocalStorage.getValue("numeroId");
            URL url = new URL(NfcTacDroidActivity.ESUP_NFC_TAG_SERVER_URL + "/desfire-ws?numeroId=" + numeroId + "&cmd=" + params[0]);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            InputStream inputStream = conn.getInputStream();
            String response = "";
            String line;
            BufferedReader br=new BufferedReader(new InputStreamReader(inputStream));
            while ((line=br.readLine()) != null) {
                response+=line;
            }
            conn.disconnect();
            return response;
        }catch (Exception e){
            throw new NfcTagDroidException(e);
        }
    }
}
