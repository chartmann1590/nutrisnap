# NutriSnap 🥭

> On-device AI calorie tracker for Android. Snap a meal, Pip identifies your food and estimates calories + macros — fully offline.

[![GitHub Pages](https://img.shields.io/badge/Website-Live-FF9F1C?style=flat-square)](https://chartmann1590.github.io/nutrisnap/)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue?style=flat-square)](LICENSE)

**Package:** `com.charles.nutrisnap` · **Min SDK:** 26 · **Target:** 34 · **Kotlin + Jetpack Compose + Material 3**

---

## Website

The marketing/landing site is in [`docs/`](docs/) and hosted on **GitHub Pages**:

**[https://chartmann1590.github.io/nutrisnap/](https://chartmann1590.github.io/nutrisnap/)**

Includes:
- Product landing page (hero, features, how-it-works, screenshots gallery, FAQ)
- [Privacy Policy](https://chartmann1590.github.io/nutrisnap/privacy-policy.html) — Play-Store-ready
- [Terms of Service](https://chartmann1590.github.io/nutrisnap/terms-of-service.html)
- Self-hosted fonts (Fredoka + Nunito) — no third-party requests
- Snackable design system (Mango #FF9F1C, Cocoa, Berry palette)

## App

A single-module Android app with on-device AI (Gemma 4 via LiteRT-LM):

```
com.charles.nutrisnap/
├── ai/           GemmaEngine + LiteRT-LM implementation
├── data/         Room DB, DataStore, repositories
├── download/     Resumable model download (WorkManager + OkHttp)
├── feature/
│   ├── onboarding/    Goal questionnaire + model download
│   ├── scan/          Camera capture + AI inference
│   ├── dashboard/     Daily rings + meal log
│   ├── history/       Diary, trends, streaks, weight
│   ├── entry/         Manual + barcode entry
│   ├── profile/       Profile settings
│   └── settings/      Goals, units, model management
├── di/           Hilt modules
└── ui/           Theme, components, nav
```

### Build

```bash
./gradlew :app:assembleDebug
./gradlew test
```

Requires a physical device (6 GB+ RAM) for the on-device AI model.

## Design

Stitch screens and design references in [`design/`](design/). The "Snackable" design system was created via **Google Stitch** and translated into a Compose theme.

## License

The app source is Apache 2.0. The Gemma 4 model is subject to the [Gemma License](https://ai.google.dev/gemma/terms).
