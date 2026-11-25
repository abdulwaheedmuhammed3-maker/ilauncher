# iLauncher for Xiaomi 14T

A native Android home-screen replacement inspired by the iPhone home-screen experience.

## What this app actually is
iLauncher registers with Android as a **HOME launcher**. Once selected as the default Home app, pressing Home returns to iLauncher instead of Xiaomi Home.

## Current implementation
- Android HOME + DEFAULT intent registration and normal app-launcher entry point.
- Real installed-app discovery through PackageManager launcher activities.
- Tap-to-launch installed Android apps.
- Four-column iPhone-inspired home icon grid.
- Multiple home pages with horizontal swipe.
- Four-icon dock, prioritising Phone, Messages, Browser and Camera where available.
- Up-swipe and App Library button to open the searchable app drawer.
- Search across installed app labels.
- Date and clock widgets.
- Basic launcher settings for wallpaper gradient and icon labels.
- Refreshes the installed app list when returning to the launcher.
- GitHub Actions debug APK build.

## Build
Open in Android Studio or use **Actions → Android debug APK** and download the `nuvia-debug-apk` artifact (the artifact label will be renamed in the next workflow adjustment). The application installed from that artifact is labelled **iLauncher**.

## Set as default on Xiaomi 14T
Install the APK, then open Settings → Apps → Manage apps → Default apps → Launcher (wording may differ by HyperOS version) and choose **iLauncher**. You can also press Home after installation and select iLauncher in Android's Home-app chooser when offered.

## Current limitations
This is the first real launcher foundation. Drag-to-rearrange icons, user-created folders, true Android widgets, icon-pack support, per-page layout customisation and a dedicated wallpaper picker are not implemented yet. iLauncher does not replace Xiaomi's secure lock screen or protected system notification shade.
