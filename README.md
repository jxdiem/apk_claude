# diem_geo

App Android per la geolocalizzazione affidabile sul campo: mappe open source
(OSM/WMS/WMTS, sorgenti aggiungibili), tracciamento di poligoni camminando,
stima dell'errore GPS e un punteggio di attendibilità della posizione
(anti-spoofing euristico), scatto foto con riconoscimento del contenuto via
AI on-device, metadati completi (coordinate, ora, sensori) e una firma
steganografica nascosta nei pixel per rilevare manomissioni delle foto.

## Build

L'APK di debug viene generato automaticamente da GitHub Actions
(`.github/workflows/android-build.yml`) ad ogni push su `main`: il workflow
compila il progetto e pubblica l'APK come artifact scaricabile dalla tab
Actions del repository.

Per compilare in locale serve JDK 17 e Android SDK (platform 34):

```
./gradlew assembleDebug
```

L'APK generato si trova in `app/build/outputs/apk/debug/`.

## Note sull'attendibilità della posizione

Android non offre alcuna API che garantisca in modo assoluto che una
posizione GPS sia genuina. Il "punteggio di attendibilità" mostrato in app
combina più segnali (flag di mock location, numero di satelliti usati nel
fix, qualità del segnale GNSS, plausibilità dei sensori di movimento) in
un indicatore euristico 0-100%, utile per segnalare posizioni sospette ma
non utilizzabile come prova legale di autenticità.
