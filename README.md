# Mika - Personal AI Companion Android App

**Mika** is a personal AI companion Android application built using Kotlin, Jetpack Compose, Material 3, and Room database. Designed for ONE user on ONE phone with complete privacy and zero backend servers.

---

## Quick Beginner Setup Guide

### 1. Downloading and Installing the APK
1. Go to the **Actions** tab in this GitHub repository.
2. Select the latest workflow run under **Build Mika APK**.
3. Scroll down to **Artifacts** and download `Mika-debug-apk.zip`.
4. Unzip the downloaded file to get `app-debug.apk`.
5. Transfer `app-debug.apk` to your phone and tap it to install. (Allow installation from unknown sources if prompted).

---

### 2. Getting an AI API Key (Groq or OpenRouter)
Mika works out of the box with any OpenAI-compatible API provider:

- **Groq (Recommended Free Tier)**:
  1. Create a free account at [api.groq.com](https://api.groq.com).
  2. Generate an API Key in the Groq console.
  3. Open Mika's **Settings** screen, paste your API Key, keep the default Base URL (`https://api.groq.com/openai/v1`), and set the model to `llama-3.3-70b-versatile`.
  4. Tap **Test Connection** to verify.

- **Vision Model Setting (For Camera & Screen Sharing)**:
  - If using Groq vision, set **Vision Model** in Settings to `llama-3.2-11b-vision-preview` or `llama-3.2-90b-vision-preview`.

---

### 3. Adding a 3D Avatar Model (.vrm)
1. Download or create a free VRM model using [VRoid Studio](https://vroid.com/en/studio).
2. Save the exported `.vrm` file on your phone.
3. In Mika, open **Settings** -> tap **Choose Avatar (.vrm)**, and pick your `.vrm` file.
4. Open the 3D Avatar screen to see Mika in full 3D!

---

## Features & Optional Configuration

- **Voice Output (TTS)**: Turn on **Speak Her Replies** in Settings to hear replies using Android TextToSpeech. Fish Audio provider is marked as "Coming soon".
- **Daily Check-ins & Reminders**: Schedule warm check-ins or ask Mika to remind you ("remind me in 10 minutes to stretch").
- **Hey Mika Wake Word**: Hands-free voice activation using Picovoice Porcupine. Requires entering your Picovoice AccessKey in Settings and enabling the RECORD_AUDIO permission.
- **Camera Vision**: Toggle **Camera Vision** to allow single-frame image captures ("look at me", "what's this?"). Features a front/rear camera toggle.
- **Live Screen Mode**: Tap **Share my screen** to let Mika follow your screen in real-time via Android MediaProjection.
- **Enrolled People Recognition**: Safe opt-in face recognition switch (marked as "Coming soon").

---

## Permissions & Privacy Overview

| Feature | Android Permission | How It Works |
| :--- | :--- | :--- |
| **Chat & AI** | `INTERNET` | All chat runs directly on your phone and communicates with your configured API URL. |
| **Check-ins & Reminders** | `POST_NOTIFICATIONS` | Local notifications only (No Firebase, no cloud tracking). |
| **Wake Word** | `RECORD_AUDIO` | On-device wake-word detection using Picovoice. |
| **Camera Vision** | `CAMERA` | Active only when app is on-screen. Captures single frames on demand. |
| **Direct Calling** | `CALL_PHONE` | Prefills or dials confirmed calls directly. |
| **Screen Share** | `MEDIA_PROJECTION` | High-priority persistent notification banner. Auto-stops on demand. |

---

## Privacy Guarantee
- **Everything runs on your phone.** No analytics, no trackers, no ads.
- Secret keys and passwords are encrypted on-device using Android Keystore-backed `EncryptedSharedPreferences`.
- Backups exported as JSON **never include API keys, passwords, or secrets**.

---

## Developer & Build Instructions

To build the APK locally, run:
```bash
./gradlew assembleDebug
```
To run all unit tests:
```bash
./gradlew test
```
