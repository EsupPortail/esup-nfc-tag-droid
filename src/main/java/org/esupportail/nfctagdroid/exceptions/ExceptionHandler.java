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
package org.esupportail.nfctagdroid.exceptions;

import android.content.Context;
import android.os.Build;
import android.os.Process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.esupportail.nfctagdroid.NfcTagDroidActivity;

import java.io.PrintWriter;
import java.io.StringWriter;

/*
 * When uncaught exception occures, we sent email to admins
 */
public class ExceptionHandler implements java.lang.Thread.UncaughtExceptionHandler {

    private final Context myContext;
    private final String LINE_SEPARATOR = "\n";
    private int i = 0;

    static private final Logger log = LoggerFactory.getLogger(NfcTagDroidActivity.class);
    static private final Marker MARKER = MarkerFactory.getMarker("NOTIFY_ADMIN");

    public ExceptionHandler(Context myContext) {
        this.myContext = myContext;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {

        synchronized (myContext) {

            StringWriter stackTrace = new StringWriter();
            ex.printStackTrace(new PrintWriter(stackTrace));

            StringBuilder errorReport = new StringBuilder();
            errorReport.append("\n************ CAUSE OF ERROR ************\n\n");
            errorReport.append(stackTrace.toString());
            errorReport.append("\n************ DEVICE INFORMATION ***********\n");
            errorReport.append("Brand: ");
            errorReport.append(Build.BRAND);
            errorReport.append(LINE_SEPARATOR);
            errorReport.append("Device: ");
            errorReport.append(Build.DEVICE);
            errorReport.append(LINE_SEPARATOR);
            errorReport.append("Model: ");
            errorReport.append(Build.MODEL);
            errorReport.append(LINE_SEPARATOR);
            errorReport.append("Id: ");
            errorReport.append(Build.ID);
            errorReport.append(LINE_SEPARATOR);
            errorReport.append("Product: ");
            errorReport.append(Build.PRODUCT);
            errorReport.append(LINE_SEPARATOR);
            errorReport.append("\n************ FIRMWARE ************\n");
            errorReport.append("SDK: ");
            errorReport.append(Build.VERSION.SDK_INT);
            errorReport.append(LINE_SEPARATOR);
            errorReport.append("Release: ");
            errorReport.append(Build.VERSION.RELEASE);
            errorReport.append(LINE_SEPARATOR);
            errorReport.append("Incremental: ");
            errorReport.append(Build.VERSION.INCREMENTAL);
            errorReport.append(LINE_SEPARATOR);

            errorReport.append("************ END OF ERROR ************");

            log.error(errorReport.toString());

            // sleep 1 sec. to let the phone sent mail and to avoid eventually emails loop freezing phone
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.trace("Exception during Thread.sleep on ExceptionHandler", e);
            }

            Process.killProcess(Process.myPid());
            System.exit(1);
        }
    }
}