# 🚀 KNotes: AI-Powered Productivity Suite

<div align="center">

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4.svg?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Clean Architecture](https://img.shields.io/badge/Architecture-MVVM%20%2B%20Clean-00C853.svg?style=for-the-badge)]()
[![Room DB](https://img.shields.io/badge/Room%20DB-v12-FF6D00.svg?style=for-the-badge&logo=sqlite&logoColor=white)]()
[![Firebase Sync](https://img.shields.io/badge/Firebase-Auth%20%2B%20Firestore-FFCA28.svg?style=for-the-badge&logo=firebase&logoColor=black)](https://firebase.google.com)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20%28API%2026%2B%29-3DDC84.svg?style=for-the-badge&logo=android&logoColor=white)](https://www.android.com)

### ✨ Capture Ideas • Manage Tasks • Track Habits • Unleash AI Intelligence

</div>

---

## 📖 About KNotes

**KNotes** is a modern, offline-first personal knowledge management and productivity application for Android. Engineered with **Kotlin**, **Jetpack Compose Material 3**, and **Clean Architecture**, KNotes seamlessly combines note-taking, task scheduling, finger/stylus canvas drawing, 30-day habit analytics, and AI assistance into a single unified workspace.

Whether you're taking markdown notes, drawing handwritten diagrams, setting recurring task deadlines, or tracking your productivity velocity, KNotes provides a fast, secure, and beautiful user experience with instant local persistence and automatic cloud synchronization.

---

## ✨ Key Feature Highlights

- 📝 **Rich Markdown & Stylus Sketch Pad**: Markdown editing with code blocks, tables, and an interactive stylus/finger drawing canvas (`SketchCanvasCompose`).
- 🎯 **Tasks & Alarm Engine**: Task deadlines, subtask progress bars (`✓ 3/5 subtasks`), recurring repeat schedules with end-dates, and high-precision alarm notifications (`TaskReminderManager`).
- 🧘 **AI Focus Digest & 528Hz Zen Timer**: Personal velocity score with a synthesized **528Hz Solfeggio Zen Meditation Bell** completion chime (`FocusAlarmPlayer`).
- 📊 **30-Day Habit Heatmap & Weekly Analytics**: Interactive 7-day bar chart, GitHub/Duolingo-style 30-day activity grid, peak productivity hour detection, and trophy badges.
- 🤖 **Dual AI Intelligence**: Powered by Google Gemini 1.5 Flash & Groq API for AI Chat, note summarization, image OCR analysis, and smart auto-tagging.
- 🔐 **Production Security & Cloud Sync**: Firebase Auth (Email/Password + Google Sign-In), biometric lock, encrypted preferences, and complete user data isolation on logout.

---

## 📸 Screenshots & Showcase

| Home Dashboard | Notes Library | Tasks & To-Dos |
| :---: | :---: | :---: |
| ![Home Screen](docs/screenshots/home_screen.png) | ![Notes Screen](docs/screenshots/notes_screen.png) | ![Tasks Screen](docs/screenshots/tasks_screen.png) |

| Habit Heatmap & Analytics | AI Assistant Chat | Stylus Sketch Pad |
| :---: | :---: | :---: |
| ![Analytics Screen](docs/screenshots/analytics_screen.png) | ![AI Chat Screen](docs/screenshots/ai_chat.png) | ![Sketch Pad Screen](docs/screenshots/sketch_canvas.png) |

---

## 🌳 System Flowcharts & Representations

### 🏛️ Clean Architecture Layers Flowchart

```mermaid
graph TD
    subgraph PRESENTATION["📱 Presentation Layer (UI & State)"]
        UI["Jetpack Compose & Material 3 Views"]
        VM["ViewModels (StateFlow / UIState)"]
        UI <--> |Observes State / Emits Events| VM
    end

    subgraph DOMAIN["🧠 Domain Layer (Pure Business Rules)"]
        UC["UseCases (GetAnalytics, SummarizeNote)"]
        DM["Domain Models (Note, Task, Folder)"]
        CR["Repository Contracts (Interfaces)"]
        VM --> |Invokes| UC
        UC --> |Operates on| DM
        UC --> |Calls| CR
    end

    subgraph DATA["💾 Data Layer (Persistence & Remote)"]
        REPO["Repository Implementations"]
        ROOM["Room Database v12 (Local Source of Truth)"]
        FIREBASE["Firebase Firestore & Auth (Cloud Sync)"]
        CR <---|Implements| REPO
        REPO <--> |0ms Offline First| ROOM
        REPO <--> |Background Auto-Sync| FIREBASE
    end
```

### 🔄 Offline-First Cloud Synchronization Flow

```mermaid
flowchart LR
    A["✏️ User Edits Note / Task"] --> B["💾 Local Room DB (v12)"]
    B --> C["⚡ StateFlow Emits to UI (0ms Latency)"]
    B --> D{"🌐 Network Connected?"}
    D -- Yes --> E["☁️ Push to Firebase Firestore"]
    D -- No --> F["⏳ Queue WorkManager Background Sync"]
    F --> |On Network Restored| E
```

---

## 🛠️ Tech Stack Matrix

| Category | Technology | Usage / Purpose |
| :--- | :--- | :--- |
| **Language** | Kotlin 2.0.21 | Coroutines, Flow, StateFlow, Sealed Interfaces |
| **UI Framework** | Jetpack Compose + Material 3 | Declarative UI, Dynamic Color, Spring Physics |
| **Architecture** | Clean Architecture + MVVM | Scalable presentation, domain, and data isolation |
| **DI Engine** | Google Hilt 2.51.1 | Type-safe compile-time dependency injection |
| **Local Database** | Room DB v12 | Local SQLite persistence, Flow observers, Migrations |
| **Cloud Backend** | Firebase Auth & Firestore | User authentication & bidirectional cloud sync |
| **AI Intelligence** | Google Gemini 1.5 Flash SDK | Summarization, Vision OCR, Auto-tagging, Q&A |
| **Security** | AndroidX Biometric + Crypto | Hardware fingerprint/face lock & EncryptedPrefs |
| **Background Tasks** | WorkManager & AlarmManager | Exact alarms, recurring reminders, boot restoration |

---

## 🗄️ Database Schema & Room Migrations

KNotes operates on an offline-first **Room Database (Version 12)**:

| Migration | Key Modifications Introduced |
| :--- | :--- |
| **`MIGRATION_10_11`** | Added `recurrenceEndDate` (`INTEGER`) column to `tasks` table for date range repeats. |
| **`MIGRATION_11_12`** | Added `lastCompletedTime` (`INTEGER`) column to `tasks` table for progress synchronization. |

---

## 🚀 Setup & Quick Start Guide

### 1️⃣ API Key Configuration
Add your Google Gemini AI API key to `local.properties`:

```properties
# local.properties
AI_API_KEY=your_gemini_api_key_here
```

### 2️⃣ Firebase Configuration
1. Create a Firebase project at [Firebase Console](https://console.firebase.google.com/).
2. Register package `com.example.knotes` and download `google-services.json` into the `app/` directory.
3. Enable **Email/Password** and **Google Sign-In** in Firebase Authentication.

### 3️⃣ Build & Run
Compile and launch the app using Gradle:
```bash
./gradlew :app:assembleDebug
```

---

<div align="center">

### ⭐ Engineered with Precision • Built for Productivity

Crafted with ❤️ by **Akash**

</div>
