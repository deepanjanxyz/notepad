# Store metadata (fastlane)

Google Play / F-Droid store listing for Elite Memo Pro, in fastlane's `metadata/android/<locale>/` layout.

## Layout

| Path | Purpose |
| :--- | :--- |
| `<locale>/title.txt` | App name shown on the store |
| `<locale>/short_description.txt` | Short store blurb |
| `<locale>/full_description.txt` | Full listing description |
| `<locale>/changelogs/<versionCode>.txt` | "What's new" text; `default.txt` is the fallback |
| `<locale>/images/` | Icon, feature graphic and phone screenshots |

## What changed in v1.0.11

Compared with the previous release (v1.0.10, which shipped **only `en-US`**), this release
rewrote the `en-US` listing and added **12 new locales**, bringing the directory to
**13 locales**: `as-IN`, `bn-IN`, `en-US`, `gu-IN`, `hi-IN`, `kn-IN`, `ml-IN`, `mr-IN`, `or-IN`, `pa-IN`, `ta-IN`, `te-IN`, `ur-IN`.

### 1. New locales

Each new locale adds a localized `title.txt` and `short_description.txt`:

| Locale | Title | Short description |
| :--- | :--- | :--- |
| `as-IN` | এলিট মেমো প্ৰো | লেবেল, চেকলিষ্ট আৰু ৰিমাইণ্ডাৰৰ সৈতে টোকা লিখক, সজাওক আৰু আঁকক। |
| `bn-IN` | এলিট মেমো প্রো | লেবেল, চেকলিস্ট এবং রিমাইন্ডার দিয়ে নোট লেখুন, গুছিয়ে রাখুন এবং আঁকুন। |
| `gu-IN` | એલીટ મેમો પ્રો | લેબલ, ચેકલિસ્ટ અને રિમાઇન્ડર સાથે નોંધો લખો, ગોઠવો અને દોરો. |
| `hi-IN` | एलीट मेमो प्रो | लेबल, चेकलिस्ट और रिमाइंडर के साथ नोट्स लिखें, व्यवस्थित करें और ड्रॉ करें। |
| `kn-IN` | ಎಲೈಟ್ ಮೆಮೊ ಪ್ರೊ | ಲೇಬಲ್, ಚೆಕ್‌ಲಿಸ್ಟ್, ರಿಮೈಂಡರ್‌ಗಳೊಂದಿಗೆ ಟಿಪ್ಪಣಿ ಬರೆಯಿರಿ, ಜೋಡಿಸಿ, ಚಿತ್ರಿಸಿ. |
| `ml-IN` | എലൈറ്റ് മെമ്മോ പ്രോ | ലേബൽ, ചെക്ക്‌ലിസ്റ്റ്, റിമൈൻഡർ ഉപയോഗിച്ച് നോട്ട് എഴുതുക, ക്രമീകരിക്കുക, വരയ്ക്കുക. |
| `mr-IN` | एलीट मेमो प्रो | लेबल, चेकलिस्ट आणि रिमाइंडर्स वापरून नोट्स लिहा, व्यवस्थित करा आणि काढा. |
| `or-IN` | ଏଲିଟ୍ ମେମୋ ପ୍ରୋ | ଲେବଲ୍, ଚେକ୍‌ଲିଷ୍ଟ୍ ଏବଂ ରିମାଇଣ୍ଡର୍ ସହିତ ନୋଟ୍ ଲେଖନ୍ତୁ, ସଜାଡ଼ନ୍ତୁ ଏବଂ ଆଙ୍କନ୍ତୁ। |
| `pa-IN` | ਇਲੀਟ ਮੈਮੋ ਪ੍ਰੋ | ਲੇਬਲਾਂ, ਚੈੱਕਲਿਸਟਾਂ ਅਤੇ ਰੀਮਾਈਂਡਰਾਂ ਨਾਲ ਨੋਟ ਲਿਖੋ, ਤਰਤੀਬ ਦਿਓ ਅਤੇ ਡਰਾਅ ਕਰੋ। |
| `ta-IN` | எலைட் மெமோ ப்ரோ | லேபிள், செக்லிஸ்ட், ரிமைண்டருடன் நோட் எழுதுங்கள், ஒழுங்காக்குங்கள், வரையுங்கள். |
| `te-IN` | ఎలైట్ మెమో ప్రో | లేబుల్స్, చెక్‌లిస్ట్‌లు, రిమైండర్‌లతో నోట్స్ రాయండి, నిర్వహించండి, గీయండి. |
| `ur-IN` | ایلیٹ میمو پرو | لیبلز، چیک لسٹس، اور ریمائنڈرز کے ساتھ نوٹس لکھیں، ترتیب دیں، اور ڈرا کریں۔ |

### 2. `en-US` listing rewritten

- **title.txt** — unchanged: `Elite Memo Pro`.
- **short_description.txt** — `Private, biometric-protected Markdown notepad.` -> `Write, organize, and draw notes with labels, checklists, and reminders.`
- **full_description.txt** — rewritten to describe the v1.0.11 feature set: text notes and checklists, freehand drawing (pen, marker, highlighter, selection, eraser, canvas backgrounds), labels, colours and pins, search and filtering, grid/list layouts, archive and Trash, reminders, light/dark/system appearance, an optional app lock, and local Room storage.
- **changelogs/** — the per-version files `4.txt`, `5.txt`, `6.txt`, `7.txt` and `10.txt` were removed and replaced by a single `default.txt`.

### 3. Images

- Added `images/featureGraphic.png`.
- Updated `images/icon.png`.
- Replaced the three old screenshots (`1.png`, `2.png`, `3.png`) with a 14-image dark set:
  `01-home-grid-dark.png`, `02-home-list-dark.png`, `03-search-results-dark.png`, `04-navigation-drawer-dark.png`, `05-note-editor-dark.png`, `06-labels-sheet-dark.png`, `07-color-picker-dark.png`, `08-checklist-editor-dark.png`, `09-drawing-note-dark.png`, `10-archive-dark.png`, `11-trash-dark.png`, `12-settings-dark.png`, `13-reminder-dialog-dark.png`, `14-reminder-applied-dark.png`.

### Notes

- `full_description.txt` exists only for `en-US`; other locales fall back to it.
- The screenshot set is dark-mode only.
- Screenshots are produced by `scripts/screenshots/capture.py` (see the store-screenshots workflow).

