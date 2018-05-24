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

import com.fasterxml.jackson.databind.ObjectMapper;

import org.esupportail.nfctagdroid.beans.NfcResultBean;
import org.esupportail.nfctagdroid.exceptions.NfcTagDroidException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.esupportail.nfctagdroid.requestasync.CsnHttpRequestAsync;
import org.esupportail.nfctagdroid.utils.HexaUtils;


public class CsnNfcProvider {

    static private final Logger log = LoggerFactory.getLogger(DesfireNfcProvider.class);
    static private final int time = 5000;

    public NfcResultBean csnRead(Tag tag) {
        log.info("Detected CSN tag with id : " + tag.getId());
        String cardId = HexaUtils.byteArrayToHexString(tag.getId());
        String cardIdArr[] = cardId.split(" ");
        CsnHttpRequestAsync task = new CsnHttpRequestAsync();
        NfcResultBean nfcResult = new NfcResultBean();
        nfcResult.setCode(NfcResultBean.CODE.ERROR);
        try {
            String response = task.execute(cardIdArr).get(time, TimeUnit.MILLISECONDS);
            nfcResult = new ObjectMapper().readValue(response, NfcResultBean.class);
        } catch (TimeoutException e){
            log.warn("Time out");
            throw new NfcTagDroidException("Time out CSN", e);
        } catch (InterruptedException e) {
            throw new NfcTagDroidException("InterruptedException", e);
        } catch (ExecutionException e) {
            throw new NfcTagDroidException("InterruptedException", e);
        }  catch (IOException e) {
            throw new NfcTagDroidException(e);
        }
        return nfcResult;
    }


}
