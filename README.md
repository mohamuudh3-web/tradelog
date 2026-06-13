# TradeLog

An offline-first trading journal & analytics app for forex / prop-firm traders.
Native Android, Kotlin + Jetpack Compose (Material 3), Room, MVVM. All data is stored
on-device — the only network call is the weekly economic-calendar feed.

## Modules
- **Dashboard** — net/today P&L, win rate, goal progress, today's tasks, recent trades.
- **Journal** — trading journal (instrument, direction, entry/exit, lot, risk %, R, result, setup tag, notes, screenshot) + daily journal (mindset/routine/reflection, mood & discipline).
- **Analytics** — win rate, avg R, profit factor, expectancy, equity curve, performance by strategy & instrument, best/worst setup.
- **Goals & Tasks** — daily/weekly goals (manual or auto-tracked from trades/journals/tasks), recurring daily task checklist.
- **Portfolio** — multiple accounts with per-account P&L.
- **Notebook** — titled, tagged, searchable notes.
- **Payouts** — payout dashboard + journal (paid/pending).
- **Tools** — position calculator (with presets) + economic calendar (Forex Factory feed, cached for offline).
- **Morning briefing** — daily notification summarizing today's high-impact events (configurable in Settings).

## Build the APK

### Option A — GitHub Actions (no local toolchain needed)
Every push to `main` runs `.github/workflows/build.yml`, which compiles a debug APK and
publishes it as a **GitHub Release asset**. Open the release on your phone and download
`app-debug.apk`.

### Option B — Local build (Android Studio or CLI)
Requires JDK 17 + Android SDK (platform 34).

```bash
./gradlew assembleDebug
# APK at: app/build/outputs/apk/debug/app-debug.apk
```

Install on a connected phone (USB debugging on):

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Stack
Kotlin · Jetpack Compose · Material 3 · Room · Coroutines/Flow · WorkManager + AlarmManager ·
Retrofit + kotlinx.serialization · Coil · DataStore. minSdk 24, target/compile SDK 34.

The database is seeded with example accounts, trades, goals, tasks, notes and payouts on
first launch so the UI is populated immediately.
