# 🔐 Elite Memo Pro

**Elite Memo Pro** is a high-performance, secure, and fully offline Android notepad application. It features a premium dark UI and advanced biometric security to keep your thoughts safe.

---

## ✨ Key Features
* 🔐 **Biometric Lock:** Secure your notes with biometric authentication or your device screen lock.
* 💾 **Auto-Save:** Never lose a word; every character is saved instantly.
* 🕒 **Timestamps:** Track when each note is created or modified.
* 🔍 **Smart Search:** Find any note quickly with the built-in search bar, scoped filters (title / content / all) and sort options (newest, oldest, title A–Z).
* 🎨 **Dynamic Themes:** Light / dark / system themes with optional Material You wallpaper colors on Android 12+.
* 📴 **Offline First:** No internet required, your data stays on your device.

---

## 📸 Screenshots
| Note List View | Dashboard View | App Settings |
| :---: | :---: | :---: |
| <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" width="200"> | <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" width="200"> | <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" width="200"> |

---

## 🛠 Technical Details
* **Language:** Kotlin (100% Kotlin codebase, no Java sources)
* **UI Toolkit:** Jetpack Compose + Material 3 (dynamic color support on Android 12+)
* **Database:** SQLite via `SQLiteOpenHelper` — same schema and storage as previous versions, so existing notes and settings are preserved
* **Security:** Biometric lock (`BiometricPrompt`) accepting any enrolled biometric or the device screen lock, re-armed whenever the app is backgrounded
* **Architecture:** Local-first, single-module, zero network permissions
* **Privacy:** Zero trackers, zero analytics, 100% private.

---

## 📄 License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

**Developed with ❤️ by Deepanjan.**
