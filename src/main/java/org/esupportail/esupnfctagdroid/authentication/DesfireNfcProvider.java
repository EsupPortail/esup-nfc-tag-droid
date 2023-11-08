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
package org.esupportail.esupnfctagdroid.authentication;

import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.IsoDep;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.esupportail.esupnfctagdroid.beans.NfcResultBean;
import org.esupportail.esupnfctagdroid.exceptions.NfcTagDroidException;
import org.esupportail.esupnfctagdroid.exceptions.NfcTagDroidInvalidTagException;
import org.esupportail.esupnfctagdroid.exceptions.NfcTagDroidPleaseRetryTagException;
import org.esupportail.esupnfctagdroid.requestasync.DesfireHttpRequestAsync;
import org.esupportail.esupnfctagdroid.utils.HexaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DesfireNfcProvider {

    static private final Logger log = LoggerFactory.getLogger(DesfireNfcProvider.class);
    static private final int time = 5000;

    public NfcResultBean desfireRead(Tag tag) throws ExecutionException, InterruptedException {

        String cardId = HexaUtils.byteArrayToHexString(tag.getId());

        NfcResultBean nfcResult = new NfcResultBean();
        nfcResult.setCode(NfcResultBean.CODE.ERROR);

        String result = "";

        IsoDep isoDep = null;
        String[] techList = tag.getTechList();
        for (String tech : techList) {
            if (tech.equals(IsoDep.class.getName())) {
                isoDep = IsoDep.get(tag);
                log.info("Detected Desfire tag with id : " + tag.getId());
            }
        }
        if (isoDep == null) {
            throw new NfcTagDroidException("Did not detect a Desfire tag ");
        }

        try {
            isoDep.connect();

            while (true) {
                DesfireHttpRequestAsync desfireHttpRequestAsync = new DesfireHttpRequestAsync();
                String response = desfireHttpRequestAsync.execute(new String[]{"result=" + result, "cardId=" + cardId}).get(time, TimeUnit.MILLISECONDS);
                try {
                    nfcResult = new ObjectMapper().readValue(response, NfcResultBean.class);
                } catch (IOException e) {
                    throw new NfcTagDroidException(e);
                }

                if(NfcResultBean.CODE.CONTINUE.equals(nfcResult.getCode())) {
                    log.warn("desfire error but esup-nfc-tag-server requests to continue ... " + result);
                    // result to empty to restart fresh apdus sequence...
                    result = "";
                } else if (!NfcResultBean.CODE.END.equals(nfcResult.getCode()) && !NfcResultBean.CODE.ERROR.equals(nfcResult.getCode())) {
                    String command = nfcResult.getFullApdu();
                    log.debug("command to send: " + command);
                    byte[] byteResult = isoDep.transceive(HexaUtils.hexStringToByteArray(command));
                    result = HexaUtils.byteArrayToHexString(byteResult);
                    log.debug("result : " + result);
                    nfcResult.setFullApdu(result);
                } else {
                    break;
                }
            }

        } catch(TimeoutException e) {
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

        return nfcResult;
    }
}
