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
package org.esupportail.nfctagdroid.authentication;

import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.IsoDep;

import org.esupportail.nfctagdroid.exceptions.NfcTagDroidException;
import org.esupportail.nfctagdroid.exceptions.NfcTagDroidPleaseRetryTagException;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.esupportail.nfctagdroid.exceptions.NfcTagDroidInvalidTagException;
import org.esupportail.nfctagdroid.requestasync.DesfireHttpRequestAsync;
import org.esupportail.nfctagdroid.utils.HexaUtils;

public class DesfireAuthProvider {

    static private final Logger log = LoggerFactory.getLogger(DesfireAuthProvider.class);
    static private final int time = 5000;

    public String desfireAuth(Tag tag) throws ExecutionException, InterruptedException {
        String response = "ERROR";

        IsoDep isoDep = null;
        String[] techList = tag.getTechList();
        for (String tech : techList) {
            if (tech.equals(IsoDep.class.getName())) {
                isoDep = IsoDep.get(tag);
                log.info("Detected Desfire tag with id : " + HexaUtils.swapPairs(tag.getId()));
            }
        }
        if (isoDep == null) {
            throw new NfcTagDroidException("Did not detect a Desfire tag ");
        }

        try {
            String[] command = new String[2];
            String result = "";
            command[1] = "1";
            isoDep.connect();

            while (!command[1].equals("OK") && !command[1].equals("ERROR")) {

                DesfireHttpRequestAsync desfireHttpRequestAsync = new DesfireHttpRequestAsync();
                response = desfireHttpRequestAsync.execute(new String[]{command[1]+"/?result=" + result}).get(time, TimeUnit.MILLISECONDS);
                try {
                    JSONArray jsonArr = new JSONArray(response);
                    command[0] = jsonArr.getString(0);
                    command[1] = jsonArr.getString(1);

                    if (!command[1].equals("OK") && !command[1].equals("ERROR")) {
                        byte[] byteResult = isoDep.transceive(HexaUtils.hexStringToByteArray(command[0]));
                        result = HexaUtils.byteArrayToHexString(byteResult);
                    }
                    log.debug("command step: " + command[1]);
                    log.debug("command to send: " + command[0]);
                    log.debug("result : " + result);
                }catch(Exception e){
                    throw new NfcTagDroidException(e);
                }
            }
            response = command[0];

        }catch(TimeoutException e) {
            log.warn("Time out");
            throw new NfcTagDroidException("Time out Desfire", e);
        } catch (TagLostException e) {
            throw new NfcTagDroidPleaseRetryTagException("TagLostException - authentication aborted", e);
        } catch (NfcTagDroidException e) {
            throw new NfcTagDroidInvalidTagException("nfctagdroidInvalidTagException - tag not valid", e);
        } catch (IOException e) {
            throw new NfcTagDroidInvalidTagException("IOException - authentication aborted", e);
        } catch (ExecutionException e) {
            throw new NfcTagDroidPleaseRetryTagException("ExecutionException - authentication aborted", e);
        } catch (InterruptedException e) {
            throw new NfcTagDroidPleaseRetryTagException("InterruptedException - authentication aborted", e);
        } finally {
            try {
                isoDep.close();
            } catch (IOException e) {
                throw new NfcTagDroidException(e);
            }
        }

        return response;
    }
}
