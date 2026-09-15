# 📝 Elite Memo Pro

**Elite Memo Pro** is a high-performance, secure, and fully offline Android notepad application. It features a premium dark UI and advanced biometric security to keep your thoughts safe.

---

## ✨ Key Features
* 🔐 **Biometric Lock:** Secure your notes with Fingerprint or PIN.
* ⚡ **Auto-Save:** Never lose a word; every character is saved instantly.
* 🕒 **Timestamps:** Track when each note is created or modified.
* 🔍 **Smart Search:** Find any note quickly with the built-in search bar.
* 🌐 **Offline First:** No internet required, your data stays on your device.

---

## 📸 Screenshots
| Note List View | Dashboard View | App Settings |
| :---: | :---: | :---: |
| <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" width="200"> | <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" width="200"> | <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" width="200"> |

---

## 🛠 Technical Details

The app is being redesigned around **Jetpack Compose**, **multi-module Gradle structure**, and **Clean Architecture** — see the [design document](docs/ARCHITECTURE.md) and issue #6 for the target architecture and migration plan.

* **Language:** Kotlin
* **UI:** Jetpack Compose + Material 3 (single-activity, Navigation Compose)
* **Architecture:** Clean Architecture — `:domain` (use cases), `:data` (repositories), `:feature:*` (Compose UI), `:core:*` (database, datastore, design system)
* **Persistence:** Room (with migration from the legacy `notes.db`) + Preferences DataStore
* **DI / Async:** Hilt, Kotlin coroutines & Flow
* **Privacy:** Zero trackers, zero analytics, 100% private.

---

## 📜 License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

**Developed with ❤️ by Deepanjan.**
