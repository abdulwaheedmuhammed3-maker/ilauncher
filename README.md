# Nuvia — Android notification experience

Nuvia is a native Android companion app for a clean, iOS-inspired notification experience. This repository starts with a real Android app, not a static design mock-up.

## Included in this first build
- Native Kotlin + Jetpack Compose app shell.
- Dark Nuvia Settings screen based on the supplied screenshot.
- Independently scrollable Settings content with a fixed bottom navigation bar.
- Notification-listener permission and draw-over-other-apps permission shortcuts.
- Adjustable floating-island display duration.
- Notification listener and a small overlay presenter.
- A GitHub Actions workflow that builds a debug APK.

## Build
Open this repository in Android Studio (Ladybug or newer), allow Gradle sync, then run the `app` configuration on an Android device.

Or use GitHub Actions: open **Actions → Android debug APK → Run workflow** (or push a commit). Download the `nuvia-debug-apk` artifact from the completed run.

## Permissions
Nuvia only displays notification content after the user explicitly enables Notification access. The floating overlay requires the Android "Display over other apps" permission. Both are optional and can be revoked in Android settings.

## Important platform boundary
Nuvia does not replace Android's protected system notification shade, secure lock screen, emergency calling, or system permission dialogs. It presents its own notification overlay and settings experience on top of Android.
