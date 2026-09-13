# Bozza per la sezione "Sicurezza dei dati" (Data Safety) di Play Console

**Importante**: questa è una bozza basata sul comportamento effettivo del
codice, non una consulenza legale. La sezione Data Safety è una
dichiarazione ufficiale verso Google: prima di inviarla, ricontrolla ogni
voce tu stesso nel form di Play Console (le domande esatte cambiano nel
tempo) e correggi se qualcosa non corrisponde più a come funziona l'app.

## Raccolta e condivisione dati

L'app **non invia dati a un server dello sviluppatore**. Le uniche
connessioni di rete sono le richieste delle tile mappa a servizi di terze
parti (OpenStreetMap, OpenTopoMap, EOX, Geoportale Nazionale, o sorgenti
WMS/WMTS aggiunte dall'utente) per scaricare le immagini della mappa
visualizzata.

Per ogni tipo di dato, il form chiede tipicamente: viene raccolto?
Condiviso con terzi? A che scopo? È obbligatorio o opzionale? Può essere
eliminato dall'utente?

| Tipo di dato | Raccolto (lascia il dispositivo)? | Condiviso con terzi? | Scopo | Eliminabile |
|---|---|---|---|---|
| Posizione precisa | No (elaborata solo sul dispositivo) | No | Funzionalità dell'app (mappa, rilievo poligoni) | Sì, eliminando i poligoni/foto o disinstallando |
| Foto | No (salvate solo localmente) | No (a meno che l'utente stesso non le condivida) | Funzionalità dell'app | Sì, dalla schermata Salvati |
| Dati dei sensori (accelerometro, magnetometro, ecc.) | No | No | Calcolo dell'attendibilità della posizione e dell'angolazione della fotocamera | Sì, con la foto/poligono associato |

Nota sulle tile mappa: le richieste alle sorgenti di mappe di terze parti
trasmettono le coordinate della porzione di mappa inquadrata (non la tua
identità), necessarie per scaricare l'immagine giusta. Se Play Console
chiede se "la posizione approssimativa" viene condivisa con terzi per il
funzionamento di servizi di mappe integrati, questa è l'unica voce per cui
potrebbe essere corretto rispondere "sì, condivisa" (con scopo
"funzionalità dell'app", non pubblicità/marketing) — valuta questa
risposta con attenzione guardando le domande esatte del form.

## Pratiche di sicurezza

- Crittografia dei dati in transito: le richieste alle sorgenti mappa
  usano HTTPS quando la sorgente lo supporta (i default preconfigurati lo
  fanno tutti); sorgenti WMS/WMTS aggiunte manualmente dall'utente
  dipendono dall'URL che l'utente stesso inserisce.
- L'utente può richiedere l'eliminazione dei dati: sì, in-app (elimina foto
  o poligoni) o disinstallando l'app.
- Nessuna raccolta dati richiesta per l'uso base dell'app (le funzioni core
  richiedono permesso fotocamera/posizione, ma il dato resta locale).

## Permessi sensibili da giustificare nel form "Contenuti dell'app"

- **ACCESS_FINE_LOCATION**: necessario per mostrare la posizione
  sulla mappa e per il rilievo dei poligoni — è il cuore della
  funzionalità dell'app, quindi la giustificazione è diretta.
- **CAMERA**: necessario per scattare le foto georeferenziate.
- Nessun permesso di localizzazione in background è richiesto (il
  tracciamento funziona solo in primo piano), quindi non serve la
  dichiarazione più severa richiesta per l'accesso alla posizione in
  background.
