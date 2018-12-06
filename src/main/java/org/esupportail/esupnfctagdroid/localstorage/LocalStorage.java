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
package org.esupportail.esupnfctagdroid.localstorage;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalStorage extends SQLiteOpenHelper {

    private static LocalStorage mInstance;

    private static SQLiteDatabase database;

    private static final Logger log = LoggerFactory.getLogger(LocalStorage.class);

    public static final String LOCALSTORAGE_TABLE_NAME = "local_storage_table";

    public static final String LOCALSTORAGE_ID = "_id";

    public static final String LOCALSTORAGE_VALUE = "value";

    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = "local_storage.db";
    private static final String DICTIONARY_TABLE_CREATE = "CREATE TABLE " + LOCALSTORAGE_TABLE_NAME
            + " (" + LOCALSTORAGE_ID + " TEXT PRIMARY KEY, "
            + LOCALSTORAGE_VALUE + " TEXT NOT NULL);";

    public static LocalStorage getInstance(Activity activity) {
        if (mInstance == null) {
            mInstance = new LocalStorage(activity.getApplicationContext());
            database = mInstance.getReadableDatabase();
        }
        return mInstance;
    }
    private LocalStorage(Context context) {
        super(context,DATABASE_NAME,null,DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DICTIONARY_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db,int oldVersion, int newVersion) {
        Log.w(LocalStorage.class.getName(),
                "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + LOCALSTORAGE_TABLE_NAME);
        onCreate(db);
    }

    public static String getValue(String valueName){
        Cursor cursor = database.query(LOCALSTORAGE_TABLE_NAME,
                null,
                LOCALSTORAGE_ID + " = ?",
                new String[]{valueName}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            return cursor.getString(1);
        }
        cursor.close();
        return "";
    }

    public static void updateValue(String key, String value){
        if(key != null && value != null)
        {
            String oldValue = getValue(key);
            ContentValues values = new ContentValues();
            values.put(LocalStorage.LOCALSTORAGE_ID, key);
            values.put(LocalStorage.LOCALSTORAGE_VALUE, value);
            if(oldValue != null && !oldValue.equals(""))
            {
                database.update(LocalStorage.LOCALSTORAGE_TABLE_NAME, values, LocalStorage.LOCALSTORAGE_ID + "='" + key + "'", null);
                log.info("update " + key + " set to : " + value);
            }
            else
            {
                log.info("insert " + key + " set to : " + value);
                database.insert(LocalStorage.LOCALSTORAGE_TABLE_NAME, null, values);
            }
        }
    }
}