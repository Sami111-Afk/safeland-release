# 🧠 Dopamine Trap (Android Client)

![Kotlin](https://img.shields.io/badge/Kotlin-2.x-purple)
![Android](https://img.shields.io/badge/Android-SDK_24--36-green)
![Firebase](https://img.shields.io/badge/Firebase-Firestore_%7C_FCM-FFCA28)
![TensorFlow](https://img.shields.io/badge/TensorFlow-Lite-orange)
![Status](https://img.shields.io/badge/Status-Active_Development-brightgreen)

**Dopamine Trap** is a sophisticated, privacy-first Android application designed to monitor, filter, and throttle digital content consumption for children. Operating through a localized VPN and Accessibility Services, it performs **on-device Machine Learning (ML)** to analyze text on screen in real-time, detecting harmful content (e.g., grooming, adult content, violence) while actively throttling addictive platforms like TikTok, Instagram, and YouTube Shorts.

## 🚀 Key Features

### 1. Dual Architecture (Parent & Child Modes)
- **Parent Mode:** A control dashboard to monitor alerts, view weekly reports, and dynamically adjust throttling settings in real-time.
- **Child Mode:** Runs silently in the background, utilizing a localized VPN and Accessibility Service to monitor content and throttle network traffic.
- **Real-Time Sync:** Powered by **Firebase Firestore** to sync settings (burst size, pause duration, enabled apps) instantly from Parent to Child.

### 2. Deep UI Monitoring (Accessibility Service)
The `DopamineAccessibilityService` hooks into the Android View hierarchy to:
- Detect when the user enters the "Shorts" tab in YouTube.
- Actively track foreground states for TikTok and Instagram.
- **Extract visible text** from the screen continuously for analysis.
- *Includes a developer "Training Mode" for automated TikTok swiping to gather dataset samples.*

### 3. On-Device Machine Learning (TensorFlow Lite)
To ensure maximum privacy, content analysis happens 100% on the device.
- Uses a custom trained `model.tflite` to classify text extracted by the Accessibility Service.
- Identifies critical risk categories: `GROOMING`, `CONTINUT_SEXUAL`, `VIOLENTA_EXTREMA`.
- Bypasses cloud APIs, meaning the child's screen data never leaves the device.

### 4. Smart Network Throttling (Local VPN)
The `DopamineVpnService` creates a local VPN tunnel that intercepts and artificially degrades network traffic for specific apps.
- **Dynamic Throttling:** When an addictive app (TikTok/Instagram) is in the foreground, or the Shorts tab is active, the VPN applies calculated latency and packet drops based on the Parent's settings (`burstBytes`, `pauseMs`).

### 5. Multi-Vector Monitoring
- **SMS & Notifications:** Monitors incoming text messages and notifications from apps like WhatsApp and Telegram for flagged keywords or grooming patterns.
- **Weekly Reports:** `WeeklyReportWorker` aggregates data and generates comprehensive reports for parents.

## 🏗️ Technical Stack

- **UI:** Jetpack Compose (Material 3).
- **Architecture:** MVVM, Coroutines, WorkManager.
- **Local Storage:** Room Database for caching content events and ML reports.
- **Cloud/Backend:** Firebase (Firestore for state sync, FCM for Parent alerts, Auth).
- **AI/ML:** TensorFlow Lite (`litert`).

## ⚙️ Setup & Installation

### Prerequisites
- Android Studio (Jellyfish or newer recommended).
- A valid `google-services.json` file placed in the `app/` directory (for Firebase).

### Building the Project
1. Clone the repository.
2. Ensure you have the ML assets in `app/src/main/assets/`:
   - `model.tflite`
   - `vocab.json`
   - `labels.json`
3. Sync Gradle and build the project.

### Permissions Required (Child Device)
For the app to function correctly on a child's device, the following must be granted:
- **VPN Service:** For network traffic interception and throttling.
- **Accessibility Service:** For reading screen text and detecting UI states.
- **Notification Access:** To monitor incoming messages.
- **SMS Permissions:** To scan text messages for risks.

## 🔒 Privacy & Security Note
*Dopamine Trap prioritizes user privacy. All screen reading, text extraction, and Machine Learning classification occur strictly on the local device. Data sent to Firebase consists only of aggregated reports, settings sync, and flagged alerts for the Parent.*
