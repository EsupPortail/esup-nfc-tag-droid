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
            isoDep.connect();
            //Phase 0
            DesfireHttpRequestAsync task0 = new DesfireHttpRequestAsync();
            String selectCmd = "";
            selectCmd = task0.execute(new String[]{"selectApp"}).get(time, TimeUnit.MILLISECONDS);
            byte[] selectedAppResult = isoDep.transceive(HexaUtils.hexStringToByteArray(selectCmd));
            log.debug("Select return : " + HexaUtils.byteArrayToHexString(selectedAppResult));

            // Phase 1
            DesfireHttpRequestAsync task1 = new DesfireHttpRequestAsync();
            String startAuthCmd = "";
            startAuthCmd = task1.execute(new String[]{"rndB"}).get(time, TimeUnit.MILLISECONDS);
            byte[] startAuthResult = isoDep.transceive(HexaUtils.hexStringToByteArray(startAuthCmd));
            log.debug("Start auth return : " + HexaUtils.byteArrayToHexString(startAuthResult));

            // Phase 2
            DesfireHttpRequestAsync task2 = new DesfireHttpRequestAsync();
            String rndB = HexaUtils.byteArrayToHexString(startAuthResult);
            String authCmd = "";
            authCmd = task2.execute(new String[]{"rndAPrimEnc&rndb=" + rndB.substring(0, rndB.length() - 4)}).get(time, TimeUnit.MILLISECONDS);
            byte[] authResult = isoDep.transceive(HexaUtils.hexStringToByteArray(authCmd));
            log.debug("Auth return : " + HexaUtils.byteArrayToHexString(authResult));

            // Phase 3
            DesfireHttpRequestAsync task3 = new DesfireHttpRequestAsync();
            String readCmd = "";
            readCmd = task3.execute(new String[]{"readFile"}).get(time, TimeUnit.MILLISECONDS);
            byte[] readResult = isoDep.transceive(HexaUtils.hexStringToByteArray(readCmd));
            log.debug("Read return : " + HexaUtils.byteArrayToHexString(readResult));

            // Phase 4
            DesfireHttpRequestAsync task4 = new DesfireHttpRequestAsync();
            String desfireId = HexaUtils.byteArrayToHexString(readResult);
            String rndAPrimEnc = HexaUtils.byteArrayToHexString(authResult);
            String getIdp2s = "desfireRequest" +
                    "&encDesfireId=" + desfireId.substring(0, desfireId.length() - 4) +
                    "&rndAPrimEnc=" + rndAPrimEnc.substring(0, rndAPrimEnc.length() - 4);

            response = task4.execute(new String[]{getIdp2s}).get(time, TimeUnit.MILLISECONDS);
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
