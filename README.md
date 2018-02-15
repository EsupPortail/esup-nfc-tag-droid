ESUP-NFC-TAG-DROID
==================

Esup-nfc-tag-droid permet d'encoder et de lire les cartes Mifare Desfire.
Le client s'appuie sur la platefome https://github.com/EsupPortail/esup-nfc-tag-server qui calcule les commandes (APDU) à transmettre à la carte.

Esup-nfc-tag-droid permet d'utiliser un smartphone Android pour badger, en utilisant l'UID (CSN) ou en faisant une lecture d'un fichier Desfire (avec autentification AES)

L'application est packagée sous la forme d'un apk qu'il faudra installer sur un smartphone Android en autorisant les applciations de sources inconnues.

L'application peut être installée et debugée depuis Android-Studio ou complilée directement à l 'aide de Gradle


## Fonctionalités

1 - L'application esup-nfc-tag-droid se comporte de la même manière que l'application Java [esup-nfc-tag-desktop] (https://github.com/EsupPortail/esup-nfc-tag-desktop "esup-nfc-tag-desktop")

2 - L'application repose sur un composant webview qui se connecte et affiche la vue fournie par esup-nfc-tag-server

3 - Après l'authentification Shibboleth il faut choisir la salle de badgeage

4 - Pour badger il suffit de poser une carte sur le lecteur nfc (à l'arrière du smartphone)

## Environnement

### Pré-requis

 * Gradle 2.10 ou +
 * Pour le dev et debug : Android SDK API level 22, Android Studio 2

### Logiciel

L'application est prévue pour tourner sous Android 5 minimun

### Materiel

Un smartphone Android équipé d'un lecteur NFC et disposant d'un accès Internet

## Compilation esup-nfc-client

 * esup-nfc-tag-droid génère des logs à destination d'un fichier de logs local au téléphone, à destination d'esupNfcTagServer (envoi de logs par POST au serveur) et à destination d'une adresse mail système. Les éléments paramétrables (mail systeme, serveur esupNfcTagServer) sont à configurer dans ce fichier src/main/assets/logback.xml
 
 * modifier src/main/assets/esupnfctag.properties pour spécifier l'adresse de votre esup-nfc-tag-server
 
 * Vous pouvez spécifier les paramètres de signature de votre APK dans build.gradle, si vous ne souhaitez pas utiliser ceux donnés par défaut (connus de tous). Vous devrez alors créé un keystore : 
```
keytool -genkey -v -keystore esup-android-apps.keystore -alias LeoDroidApp -keyalg RSA -keysize 2048 -validity 10000
```
 * build de l'APK
```
gradle clean assemble
```
## Integration dans esup-nfc-tag-server

 * copier l'APK dans EsupNfcTagServer pour le mettre à disposition des utilisateurs :
```
cp ./build/outputs/apk/esupnfctagdroid-release.apk /<path to>/esup-nfc-tag-server/src/main/resources/apk/esupnfctagdroid.apk
```
 * recompiler et redéployer esup-nfc-tag-server. Au redémarrage d'esup-nfc-tag-server la nouvelle version de l'apk sera prise en compte
