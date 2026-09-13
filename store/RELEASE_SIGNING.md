# Firma di release e pubblicazione

## La keystore

Il file `diem_geo-upload.jks` (generato una sola volta) è la chiave con cui
firmi i pacchetti che carichi su Google Play. Da novembre 2021 Google Play
richiede *Play App Signing*: questa è quindi una "chiave di upload", non la
chiave finale con cui l'app viene distribuita agli utenti — quella la
gestisce Google. Se perdi la chiave di upload puoi comunque chiedere il
reset all'assistenza Play Console, ma è comunque da trattare come un
segreto: **non va mai committata nel repository** (infatti `.gitignore`
esclude `*.jks` e `keystore.properties`).

Conserva in un posto sicuro (password manager, non email/chat):
- il file `diem_geo-upload.jks`
- la password della keystore
- l'alias della chiave
- la password della chiave

## Build locale firmata

Copia `keystore.properties.example` in `keystore.properties` (nella root del
progetto, accanto a `settings.gradle.kts`) e compila i 4 valori:

```
storeFile=/percorso/assoluto/diem_geo-upload.jks
storePassword=...
keyAlias=...
keyPassword=...
```

Poi: `./gradlew bundleRelease` produce l'AAB firmato in
`app/build/outputs/bundle/release/`.

## Build firmata via GitHub Actions

Il workflow `.github/workflows/android-release.yml` (avvio manuale dalla tab
Actions → "Build Signed Release (AAB)" → "Run workflow") fa la stessa cosa
in CI, leggendo la keystore da un GitHub Secret invece che da un file
locale. Prima di usarlo, imposta questi secrets del repository
(Settings → Secrets and variables → Actions → New repository secret):

| Nome secret | Valore |
|---|---|
| `RELEASE_KEYSTORE_BASE64` | il file `.jks` codificato in base64 (una riga) |
| `RELEASE_STORE_PASSWORD` | password della keystore |
| `RELEASE_KEY_ALIAS` | alias della chiave |
| `RELEASE_KEY_PASSWORD` | password della chiave |

Per ottenere il valore base64 del file su Windows (PowerShell):

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("diem_geo-upload.jks")) | Set-Clipboard
```

(il risultato finisce negli appunti, pronto da incollare nel campo del
secret).

Il workflow produce due artifact scaricabili dalla pagina della run:
- `diem_geo-release-aab`: il file `.aab` da caricare su Play Console
- `diem_geo-release-apk`: un APK firmato equivalente, utile per installarlo
  a mano su un telefono per un ultimo controllo prima di pubblicare
