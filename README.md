Esupnfctagdroid is the Android part of the EsupNfcTag project
============================

Please read the documentation here (in French) :
https://www.esup-portail.org/wiki/display/ESUPNFC/EsupNfcTagDroid

Esupnfctagdroid should be used with EsupNfcTagServer

Esupnfctagdroid allows the use of an android to swipe a tag.

Esupnfctagdroid can read directly the Tag Serial Number (UID) or read an identifiant protected by an AES Mifare Desfire Authentication.

The main part of the GUI is provided to Esupnfctagdroid by EsupNfcTagServer inside a webview (standard web HTML application).


### Configuration and Installation

 * modify src/main/assets/logback.xml
 
 * modify src/main/assets/esupnfctag.properties

 * build the APK
```
gradle clean assemble
```

 * copy the APK in EsupNfcTagServer so that users can download it :
cp ./build/outputs/apk/esupnfctagdroid-release.apk /opt/esup-nfc-tag-server/src/main/resources/apk/esupnfctagdroid.apk

 * redeploy EsupNfcTagServer


### Debug and dev

Use Android Studio to debug or make development in Esupnfctagdroid !
