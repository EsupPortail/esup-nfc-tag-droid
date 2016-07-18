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

import org.esupportail.nfctagdroid.exceptions.NfcTagDroidException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.esupportail.nfctagdroid.requestasync.CsnHttpRequestAsync;
import org.esupportail.nfctagdroid.utils.HexaUtils;


public class CsnAuthProvider {

    static private final Logger log = LoggerFactory.getLogger(DesfireAuthProvider.class);
    static private final int time = 5000;

    public String csnAuth(Tag tag) {
        log.info("Detected CSN tag with id : " + HexaUtils.swapPairs(tag.getId()));
        String cardId = HexaUtils.byteArrayToHexString(tag.getId());
        String cardIdArr[] = cardId.split(" ");
        CsnHttpRequestAsync task = new CsnHttpRequestAsync();
        String response = "ERROR";
        try {
            response = task.execute(cardIdArr).get(time, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e){
            log.warn("Time out");
            throw new NfcTagDroidException("Time out CSN", e);
        } catch (InterruptedException e) {
            throw new NfcTagDroidException("InterruptedException", e);
        } catch (ExecutionException e) {
            throw new NfcTagDroidException("InterruptedException", e);
        }
        return response;
    }


}
