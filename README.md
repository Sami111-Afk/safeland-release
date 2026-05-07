# Safeland — Android

![Kotlin](https://img.shields.io/badge/Kotlin-2.x-purple)
![Android](https://img.shields.io/badge/Android-API_24--36-green)
![Firebase](https://img.shields.io/badge/Firebase-Firestore_%7C_FCM-FFCA28)
![TFLite](https://img.shields.io/badge/TensorFlow-Lite-orange)
![Status](https://img.shields.io/badge/Status-v1.0_Released-brightgreen)

**Safeland** este o aplicație Android de control parental care protejează copiii de conținut adictiv și dăunător pe rețelele sociale, fără a compromite intimitatea datelor.

---

## Cum funcționează

Aplicația rulează în două moduri pe dispozitive diferite:

- **Modul Copil** — serviciu VPN local care throttle-uiește TikTok, Instagram, YouTube Shorts, YouTube și Facebook pe baza limitelor de timp setate de părinte. Monitoring content prin Accessibility Service + ML on-device.
- **Modul Părinte** — dashboard real-time cu alerte, control per-aplicație, limite de timp și rapoarte de wellbeing.

Cele două dispozitive se sincronizează prin Firebase Firestore. Un cod de pairing de 6 cifre conectează conturile fără a expune date personale.

---

## Funcționalități principale

| Funcție | Detalii |
|---|---|
| VPN throttle | Degradare adaptivă a rețelei pentru apps adictive |
| Limite de timp | 0–120 min/zi per aplicație, alerte la 80% și 100% |
| Content analysis | TFLite on-device: GROOMING, SEXUAL_CONTENT, EXTREME_VIOLENCE |
| Alerte real-time | Push instant în ParentScreen via Firestore snapshot listener |
| Wellbeing engine | Focus Score, Social Risk, Consumption Quality |
| PinLock | Ecran de blocare cu cod 6 cifre setat de părinte |
| FCM | Notificări push pentru alerte critice |

---

## Stack tehnic

- **UI:** Jetpack Compose + Material 3
- **Arhitectură:** MVVM, Kotlin Coroutines, WorkManager
- **Baza de date locală:** Room
- **Sync cloud:** Firebase Firestore + FCM
- **ML:** TensorFlow Lite / LiteRT (clasificare text on-device)
- **Rețea:** Android VPN API (LocalVPN, fără server extern)
- **Auth:** Firebase Anonymous Authentication

---

## Setup pentru development

1. Clonează repo-ul
2. Pune `google-services.json` în `app/`
3. Asigură-te că assets-urile ML sunt în `app/src/main/assets/`: `model.tflite`, `vocab.json`, `labels.json`
4. Build cu Android Studio Jellyfish+

Pentru release APK:
```bash
keytool -genkey -v -keystore app/safeland-release.jks -alias safeland -keyalg RSA -keysize 2048 -validity 10000
KEYSTORE_PASS=xxx KEY_PASS=xxx ./gradlew assembleRelease
```

---

## Structură Firestore

```
families/{familyId}/
  config/parent              ← FCM token parinte
  children/{childId}/
    config/settings          ← setari trimise de parinte
    alerts/{alertId}         ← alerte sistem (time_limit, vpn_stopped, content)
    reports/{reportId}       ← rapoarte zilnice wellbeing
    wellbeing/latest         ← profil wellbeing curent

pairing/{code}               ← cod temporar de conectare (15 min)
feedback/{docId}             ← feedback suport
```

---

## Privacy

Tot procesarea de continut se face **on-device**. Datele nu parasesc telefonul copilului decat daca o alerta critica este detectata. Nicio captura de ecran, nicio inregistrare audio sau video.
