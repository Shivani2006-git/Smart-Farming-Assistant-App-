# GreenMate Android App

A native Android starter app for the Smart Farming Assistant mini-project report.

## Technology
- Java, Android SDK, Android Studio
- Minimum Android version: Android 6.0 (API 23)
- Weather: Open-Meteo geocoding and forecast APIs (no API key required)
- Reminder storage: SharedPreferences (local device storage)
- Reminder alerts: AlarmManager + NotificationManager

## Features
1. Home dashboard with navigation.
2. Weather lookup by city/town/village; current conditions and 5-day forecast.
3. General crop calendar guides for Rice, Maize, Wheat, Cotton and Tomato.
4. Add, edit and delete farming reminders; scheduled Android notifications.

## Open and run
1. Install Android Studio (latest stable) and Android SDK Platform 35.
2. Extract `GreenMateAndroid.zip`.
3. In Android Studio choose **Open**, then select the extracted `GreenMateAndroid` folder.
4. Allow Gradle sync to complete.
5. Connect an Android phone with USB debugging enabled, or create an emulator.
6. Press **Run**.

## Notes / limitations
- The weather feature requires internet access and uses Open-Meteo.
- Crop calendars are illustrative, general guides, not crop-specific official recommendations.
- Reminders are stored locally on the device. Android may delay inexact alarms due to battery management; exact alarms are intentionally not requested.
- On Android 13+, grant notification permission when prompted.
- This is an Android Studio source project, not a signed APK. Build an APK from Android Studio using Build > Build Bundle(s) / APK(s) > Build APK(s).
- The project follows the report's stated scope: weather, crop calendar and reminders. It does not automate irrigation, diagnose disease or guarantee yield.
