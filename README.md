# 🛡️ Call Blocker for Android

A lightweight, modern Android application that **instantly blocks incoming phone calls from numbers not saved in your contacts**.

Features a **toggle button directly in the notification shade / Quick Settings (just like Wi-Fi and Bluetooth)**, allowing you to turn call blocking on or off in a single tap without opening the app.

---

## ✨ Features

- **⚡ Instant Call Blocking**: Automatically rejects incoming calls from any phone number that isn't saved in your address book.
- **🔇 Zero Ringing & Distraction**: Suppresses incoming call ringing and notification sounds so you are never disturbed.
- **🔘 Toggle Like Wi-Fi / Bluetooth (Quick Settings Tile)**: Add a quick toggle tile to your notification pull-down menu right beside Wi-Fi, Bluetooth, and Flashlight.
- **📲 Notification Action Button**: An ongoing notification in your notification tray allows 1-tap toggling between "Active" and "Paused".
- **📋 Blocked Calls History**: View a log of recently blocked calls (number & timestamp) with the ability to clear history.
- **🔋 Battery Efficient & Clean**: Uses the official Android Telecom `CallScreeningService` API—it only activates when an incoming call arrives and consumes zero battery in the background.

---

## 🏗️ Project Architecture

```
CallBlocker/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/simplecallblocker/
│   │   │   ├── CallBlockerScreeningService.java  # CallScreeningService: catches & rejects unknown calls
│   │   │   ├── CallBlockerTileService.java       # TileService: Quick Settings tile (like Wi-Fi/BT)
│   │   │   ├── NotificationHelper.java           # Ongoing notification & blocked alerts
│   │   │   ├── ToggleReceiver.java               # Handles notification action button clicks
│   │   │   ├── BootReceiver.java                 # Restores notification on reboot
│   │   │   ├── BlockerPreferences.java           # SharedPreferences storage & state management
│   │   │   ├── BlockedCallItem.java              # Data model for blocked call log
│   │   │   ├── BlockedCallsAdapter.java          # RecyclerView adapter for call history
│   │   │   └── MainActivity.java                 # User interface & permission setup checklist
│   │   └── res/
│   │       ├── layout/                           # activity_main.xml & item_blocked_call.xml
│   │       ├── values/                           # colors, strings, themes (Light & Dark)
│   │       └── drawable/                         # Vector icons & card backgrounds
├── build.gradle
├── settings.gradle
└── gradlew
```

---

## ⚙️ How It Works Under The Hood

1. **Call Screening (`CallScreeningService`)**:
   - When an incoming call arrives, Android Telecom binds to `CallBlockerScreeningService.onScreenCall()`.
   - The app verifies whether the blocker is enabled.
   - The incoming phone number is queried against `ContactsContract.PhoneLookup`.
   - If the number is **not found** in contacts:
     ```java
     CallResponse.Builder response = new CallResponse.Builder()
         .setDisallowCall(true)        // Drops incoming connection
         .setRejectCall(true)          // Disconnects immediately
         .setSkipNotification(true)    // Suppresses ringing and incoming heads-up
         .setSkipCallLog(false);       // Keeps call in phone log so you know someone called
     respondToCall(callDetails, response.build());
     ```
   - If the number is in contacts, the call passes through completely untouched.

2. **Quick Settings Tile (`TileService`)**:
   - `CallBlockerTileService` registers with the Android System UI.
   - Tapping the tile immediately flips the blocker state, updates tile appearance (Active/Inactive), and synchronizes with the notification and app UI.

3. **Notification Toggle Button**:
   - `NotificationHelper` creates an ongoing notification with an action button (`[ Pause ]` or `[ Activate ]`).
   - Tapping the button fires `ToggleReceiver` without bringing any activity to the foreground.

---

## 🚀 How to Build & Install

### Option A: Using Android Studio
1. Launch **Android Studio**.
2. Select **Open** and choose the `CallBlocker` directory.
3. Allow Gradle to sync.
4. Connect your Android phone (or launch an emulator) with USB debugging enabled.
5. Click **Run ▶ (Shift + F10)**.

### Option B: Using Gradle CLI
```bash
cd CallBlocker
./gradlew assembleDebug
```
The output APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

To install directly to a connected device via ADB:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 📱 First-Time Setup Instructions

1. **Open Call Blocker**:
   - Open the app on your phone.
2. **Grant Permissions (3 Simple Steps shown on screen)**:
   - **Step 1 - Call Screening App**: Tap "Grant" and select **Call Blocker** as your default Caller ID & Spam / Call Screening app.
   - **Step 2 - Contacts**: Tap "Grant" so the app can verify saved contacts.
   - **Step 3 - Notifications**: Tap "Grant" (required on Android 13+) to enable the toggle notification.
3. **Add the Quick Settings Tile (Like Wi-Fi / Bluetooth)**:
   - Swipe down twice from the top of your screen to open the full Quick Settings panel.
   - Tap the **Edit / Pencil icon** (usually in the bottom or top corner).
   - Scroll down to find the **"Block Unknowns"** tile.
   - Drag and drop it into your active tiles list next to Wi-Fi and Bluetooth.
   - Done! You can now toggle call blocking anytime right from your quick settings panel.
