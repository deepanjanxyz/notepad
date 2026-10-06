# Privacy Policy — Elite Memo Pro

This policy describes how **Elite Memo Pro** (`com.deepanjanxyz.notepad`) handles your data. It reflects how the app actually behaves as built from this repository: every statement below is based on the app's source code, its `AndroidManifest.xml` files, its Gradle configuration and the signed APK published in the Releases section.

Last updated: 6 October 2026

## Summary

Elite Memo Pro is a local-first notes app. Your notes, drawings, labels, reminders and settings are stored on your device. The app does not request the `INTERNET` permission, so it cannot open a network connection or send data over the internet. It contains no analytics, advertising, tracking or crash-reporting components.

## What the app stores on your device

All app data is written to storage that is private to the app:

| Data | Where it is stored |
| :--- | :--- |
| Notes — text, checklists and serialised drawings, including pin state, colour, labels, archive, trash and reminder times | Room database `notes.db` |
| Labels | Room database `notes.db` (`labels_table`) |
| Theme, grid/list layout, and whether the app lock is enabled | Preferences DataStore `notepad_settings` |

Notes are not encrypted at rest by the app. They are protected by Android's app sandbox — another app cannot read this app's private storage — and by any device encryption you have enabled.

The app also contains code for an optional, encrypted backend configuration (`EncryptedSetupStore`, DataStore `notepad_setup`, using AES-GCM with a key held in the Android Keystore). That record is written only if the backend setup flow is used; see "Network access and data transmission" below.

## Network access and data transmission

**The app does not request the `INTERNET` permission.** No `AndroidManifest.xml` in this repository declares it, and the signed APK published in the Releases section does not contain it either. On Android an app cannot open a network socket without this permission, so the app is unable to send your notes, drawings, labels or settings — or any other data — over the internet.

The permissions actually present in the installed app are:

- `android.permission.USE_BIOMETRIC`
- `android.permission.POST_NOTIFICATIONS`
- `android.permission.VIBRATE`
- `android.permission.ACCESS_NETWORK_STATE` — contributed by a dependency, not requested by the app itself. It allows reading whether a network is available; it does not grant network access.

The repository does contain code that would use the network if it were enabled: a Supabase/PostgREST client, email/password and email-OTP authentication, and a backend-URL reachability check in `SetupRepository`. Nothing in the app constructs or calls that code — the setup dialog that would drive it is defined but never shown — so it is not reachable from the current UI. Even if it were reachable, the missing `INTERNET` permission would prevent it from connecting.

The app can hand a web address to another app: the Settings screen offers "GitHub" and "Issues" links, which are opened through an Android `ACTION_VIEW` intent. In that case the browser — not Elite Memo Pro — fetches the page, under the browser's own privacy policy.

## Analytics, advertising, tracking and crash reporting

None. The app bundles no analytics, advertising, tracking or crash-reporting SDK — there is no Firebase, Crashlytics, Google Play Services, ads, Sentry or comparable dependency in the Gradle files — and no such code exists in the source.

## Android permissions

| Permission | Why it is requested |
| :--- | :--- |
| `USE_BIOMETRIC` | To show the biometric or device-credential prompt when the optional app lock is enabled. |
| `POST_NOTIFICATIONS` | To post a reminder notification for a note you have scheduled. Requested on Android 13 and later. |
| `VIBRATE` | To let the reminder notification vibrate. |

The app requests no camera, microphone, location, contacts, phone, or external-storage/media permissions.

## App lock (biometrics and device credential)

The optional app lock uses Android's `BiometricPrompt` with `BIOMETRIC_STRONG` or `DEVICE_CREDENTIAL`. The prompt is presented and evaluated by Android, and the app receives only a success or failure callback. The app does not receive, process or store your fingerprint, face data, PIN, pattern or password, and it cannot read them from the operating system.

The only thing the app stores for this feature is a single boolean value recording whether the lock is enabled. If the platform cannot present the prompt at all, the app falls back to unlocking.

## Reminders and notifications

Reminders are scheduled locally with WorkManager. When a reminder fires, a worker builds a notification containing the note's title and a short preview of its content and posts it through Android's notification system. This happens entirely on the device. If the note has been deleted or moved to trash by the time the reminder fires, no notification is shown.

## Backup and device transfer

The app declares `android:allowBackup="true"`, and its backup rules include the `database` and `sharedpref` domains for both cloud backup and device-to-device transfer. This means Android's own backup service — controlled by your device settings and, for cloud backup, by your Google account — can include the app's notes database and shared preferences. That transfer is carried out by the operating system, not by the app, and is outside the app's control. To prevent it, turn off backup for this app in your device settings.

## Export, import and sharing

The app has no export, import or share feature for your notes. It does not write your notes to shared storage, and it does not read files from your device.

## Data sharing with third parties

The app does not share your data with anyone. It contains no third-party analytics, advertising or tracking components, and it does not transmit data to any server.

## Developer access to your content

The developer has no access to your notes, drawings, labels or settings. There is no account, no sync service and no server component in the app, and the app cannot send your content anywhere.

## Data deletion and retention

You control your data from within the app:

- Deleting a note moves it to **Trash**; trashing is not permanent until the trash is emptied or the note is deleted permanently.
- Emptying the trash, or permanently deleting a note, removes it from the app's database.
- Uninstalling the app, or clearing its data from Android's app settings, removes everything the app stored, including the notes database and its settings.

The developer holds no copy of your data, so there is nothing to ask the developer to delete. If Android's backup has copied the app's data (see "Backup and device transfer"), that copy is managed by your device or account and can be removed through your Android or Google account backup settings.

## Children's privacy

The app is a general-purpose notes application and is not directed at children. It has no account or sign-in and gathers no personal information from any user, including children.

## Changes to this policy

Changes to this policy are committed to this repository, so the file's history shows what changed and when. Because the app's behaviour is defined by the code in this repository, this policy will be updated whenever the app begins handling data differently.

## Contact

This project is hosted on GitHub. For privacy questions, open an issue at:

https://github.com/deepanjanxyz/notepad/issues

The repository does not publish a dedicated email address for privacy enquiries.

## Verifying these statements

Each claim above can be checked against the repository:

- Requested permissions — `app/src/main/AndroidManifest.xml`
- Local storage — `data/src/main/java/com/deepanjanxyz/notepad/data/local/` (Room database and DataStore)
- App lock — `app/src/main/java/com/deepanjanxyz/notepad/MainActivity.kt`
- Reminders — `app/src/main/java/com/deepanjanxyz/notepad/worker/`
- Backup rules — `app/src/main/res/xml/backup_rules.xml` and `app/src/main/res/xml/data_extraction_rules.xml`
- Dependencies — `app/build.gradle.kts`, `core/build.gradle.kts`, `data/build.gradle.kts`, `features/build.gradle.kts`
