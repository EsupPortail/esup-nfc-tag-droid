ESUP-NFC-TAG-DROID
==================

Esup-nfc-tag-droid permet d'encoder et de lire les cartes Mifare Desfire.
Le client s'appuie sur la platefome https://github.com/EsupPortail/esup-nfc-tag-server qui calcule les commandes (APDU) à transmettre à la carte.

Esup-nfc-tag-droid permet d'utiliser un smartphone Android pour badger, en utilisant l'UID (CSN) ou en faisant une lecture d'un fichier Desfire (avec autentification AES)

L'application est packagée sous la forme d'un apk qu'il faudra installer sur un smartphone Android en autorisant les applciations de sources inconnues.

## Fonctionalités

1 - L'application esup-nfc-tag-droid se comporte de la même manière que l'application Java [esup-nfc-tag-desktop] (https://github.com/EsupPortail/esup-nfc-tag-desktop "esup-nfc-tag-desktop")

2 - L'application repose sur un composant webview qui se connecte et affiche la vue fournie par esup-nfc-tag-server

3 - Après l'authentification Shibboleth il faut choisir la salle de badgeage

4 - Pour badger il suffit de poser une carte sur le lecteur nfc (à l'arrière du smartphone)

## Environnement

### Logiciel

L'application est prévue pour tourner sous Android 5 minimun

### Materiel

Un smartphone Android équipé d'un lecteur NFC et disposant d'un accès Internet

## Compilation esup-nfc-client

 * modifier src/main/assets/logback.xml
 
 * modifier src/main/assets/esupnfctag.properties pour spécifier l'adresse de votre esup-nfc-tag-server

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
