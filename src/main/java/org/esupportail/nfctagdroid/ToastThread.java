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
package org.esupportail.nfctagdroid;

import android.content.Context;
import android.media.MediaPlayer;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

public class ToastThread implements Runnable {

    private static ToastThread toastThread;

    protected Context context;

    protected int rStatus;

    protected String toastText;

    public static synchronized ToastThread getInstance(Context context, int rStatus, String toastText) {
        if (toastThread == null) {
            toastThread = new ToastThread();
        }
        toastThread.context = context;
        toastThread.rStatus = rStatus;
        toastThread.toastText = toastText;
        return toastThread;
    }

    public void run() {
        if(rStatus == R.raw.fail) {
            Toast toast = Toast.makeText(context, toastText, Toast.LENGTH_SHORT);
            toast.show();
        }
    }
}
