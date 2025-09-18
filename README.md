# ShiftTracker (Android, Kotlin, Compose, Room)
Минималистичный учёт смен и зарплаты. Валюта ₽. Android 14.

## Запуск (Android Studio)
1) Открой корень `ShiftTracker` (там `settings.gradle`).
2) File → Settings → Build Tools → Gradle → **Gradle JDK = 17** (Download JDK… → 17).
3) File → **Sync Project with Gradle Files**.
4) Build → **Build APK(s)** → `app/build/outputs/apk/debug/app-debug.apk`.

## CI (GitHub Actions)
В `.github/workflows/build-apk.yml` сборка через **Gradle 8.5** (скачивается напрямую), Android SDK API 34.
