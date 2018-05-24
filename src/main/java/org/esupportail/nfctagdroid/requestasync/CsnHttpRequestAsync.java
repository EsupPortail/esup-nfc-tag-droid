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

import com.fasterxml.jackson.databind.ObjectMapper;

import org.esupportail.nfctagdroid.beans.CsnMessageBean;
import org.esupportail.nfctagdroid.exceptions.NfcTagDroidException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;

import org.esupportail.nfctagdroid.NfcTagDroidActivity;
import org.esupportail.nfctagdroid.localstorage.LocalStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CsnHttpRequestAsync extends AsyncTask<String, String, String> {

    static private final Logger log = LoggerFactory.getLogger(CsnHttpRequestAsync.class);

    protected String doInBackground(String... params)  {

        CsnMessageBean nfcMsg = new CsnMessageBean();
        nfcMsg.setNumeroId(LocalStorage.getValue("numeroId"));
        nfcMsg.setCsn(params[0]);
        ObjectMapper mapper = new ObjectMapper();
        String jsonInString = null;
        try {
            jsonInString = mapper.writeValueAsString(nfcMsg);
            URL url = new URL(NfcTagDroidActivity.ESUP_NFC_TAG_SERVER_URL + "/csn-ws");
            log.info("Will call csn-ws on : " + url);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json;charset=utf-8");
            conn.connect();
            Writer writer = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream(), "UTF-8"));
            writer.write(jsonInString);
            writer.close();
            InputStream inputStream = conn.getInputStream();
            String response = "";
            String line;
            BufferedReader br=new BufferedReader(new InputStreamReader(inputStream));
            while ((line=br.readLine()) != null) {
                response+=line;
            }
            conn.disconnect();
            return response;
        } catch (Exception e) {
            throw new NfcTagDroidException(e);
        }
    }
}
