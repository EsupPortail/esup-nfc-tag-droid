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
package org.esupportail.esupnfctagdroid.logback;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.esupportail.esupnfctagdroid.beans.LogBean;
import org.esupportail.esupnfctagdroid.exceptions.NfcTagDroidException;
import org.esupportail.esupnfctagdroid.localstorage.LocalStorage;
import org.esupportail.esupnfctagdroid.requestasync.LogHttpRequestAsync;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class HttpAppender<E> extends AbstractHttpAppender<E> {

    static private final int time = 2000;

    @Override
    protected void append(E eventObject) {
         doPost(eventObject.toString());
    }

    private void doPost(final String event) {

        LogBean nfctagdroidlog = new LogBean();
        nfctagdroidlog.setNumeroId(LocalStorage.getValue("numeroId"));
        nfctagdroidlog.setErrorLevel(event.split("]")[0].replace("[", ""));
        nfctagdroidlog.setErrorReport(event.split("]")[1].trim());
        ObjectMapper mapper = new ObjectMapper();
        String jsonInString = null;
        try {
            jsonInString = mapper.writeValueAsString(nfctagdroidlog);
        } catch (JsonProcessingException e) {
            throw new NfcTagDroidException("", e);
        }
        final String[] params = new String[2];
        params[0] = getEndpoint();
        params[1] = jsonInString;
        LogHttpRequestAsync task = new LogHttpRequestAsync();
        try {
            task.execute(params).get(time, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e){
            throw new NfcTagDroidException("Time out Log", e);
        } catch (InterruptedException e) {
            throw new NfcTagDroidException("InterruptedException", e);
        } catch (ExecutionException e) {
            throw new NfcTagDroidException("ExecutionException", e);
        }

    }

}
