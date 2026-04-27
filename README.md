# 🛡️ Safeland (formerly Dopamine Trap)

![Kotlin](https://img.shields.io/badge/Kotlin-2.x-purple)
![Android](https://img.shields.io/badge/Android-SDK_24--36-green)
![Firebase](https://img.shields.io/badge/Firebase-Firestore_%7C_FCM-FFCA28)
![TensorFlow](https://img.shields.io/badge/TensorFlow-Lite-orange)
![Status](https://img.shields.io/badge/Status-Active_Development-brightgreen)

**Safeland** is a next-generation, privacy-first Android ecosystem designed to foster healthy digital habits for children. By combining real-time content analysis with advanced network throttling and psychological well-being metrics, Safeland provides parents with deep insights and proactive tools to protect their children from addictive patterns and harmful content.

## 🌟 What's New: The "Wellbeing" Update
We've moved beyond simple blocking. Safeland now features a **Digital Wellbeing Engine** that analyzes usage patterns to generate a holistic health profile for the child, providing AI-powered suggestions for a more balanced digital life.

## 🚀 Key Features

### 1. Digital Wellbeing & Analytics (New!)
- **Wellbeing Profile:** Automatically calculates metrics like "Focus Score", "Social Risk", and "Consumption Quality".
- **Session Tracking:** Precise monitoring of time spent on addictive platforms (TikTok, Instagram, YouTube Shorts).
- **Daily Progress:** Visual tracking of how digital habits evolve over time.

### 2. Dual Architecture & Modern UI
- **Parent Dashboard:** Completely redesigned with a modern, tabbed interface for seamless monitoring and control.
- **Child Mode:** A silent, battery-efficient background service.
- **Daily Limits:** Set specific time allowances for monitored apps directly from the Parent device.

### 3. Deep UI Monitoring (Accessibility Service)
- Detects specific UI states (e.g., entering "Shorts" in YouTube).
- Extracts visible text for real-time safety classification.
- Intelligent foreground detection to trigger throttling only when needed.

### 4. On-Device Machine Learning (TensorFlow Lite)
Privacy is non-negotiable. All content analysis happens 100% on-device:
- Classifies extracted text into risk categories: `GROOMING`, `SEXUAL_CONTENT`, `EXTREME_VIOLENCE`.
- Data never leaves the device unless a critical alert is triggered.

### 5. Smart Network Throttling (Local VPN)
- **Adaptive Latency:** Artificially degrades network performance for addictive apps based on usage limits.
- **Burst Control:** Parents can configure how much data is allowed before a "cooldown" pause is enforced.

## 🏗️ Technical Stack

- **UI:** Jetpack Compose (Modern Material 3 with Custom Brand Identity).
- **Architecture:** MVVM, Coroutines, WorkManager.
- **Database:** Room (Local Events/Reports), Firebase Firestore (Real-time Sync).
- **Messaging:** FCM (Data-driven alerts and reports).
- **ML:** TensorFlow Lite / LiteRT.

## ⚙️ Setup & Installation

1. Clone the repository.
2. Place your `google-services.json` in the `app/` directory.
3. Ensure ML assets (`model.tflite`, `vocab.json`, `labels.json`) are in `app/src/main/assets/`.
4. Build using Android Studio Jellyfish+.

## 🔒 Privacy Commitment
Safeland is built on the principle that a child's screen data belongs to them. Classification is local, alerts are encrypted, and monitoring is transparent.
