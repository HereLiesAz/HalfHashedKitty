# Half-Hashed Kitty - Android Client

The `app` module contains the Android client for Half-Hashed Kitty. It allows users to control the desktop application remotely via a WebSocket relay.

## Prerequisites

*   Android SDK.
*   Java JDK 17 (for building).
*   Android Studio (recommended).

## Building

```bash
# Build Debug APK
./gradlew :app:assembleDebug

# Run Unit Tests
./gradlew :app:testDebugUnitTest
```

Note: Run these commands from the repository root.

## Architecture

The app uses Jetpack Compose for UI and follows an MVVM architecture.

*   **UI Layer:** `ui/screens/` and `ui/tabs/` (Compose functions).
*   **ViewModel Layer:** `MainViewModel`, `SniffViewModel`, etc.
*   **Data Layer:** `RelayService` (WebSocket handling), API clients.

## Features

*   **QR Code Scanning:** Quickly join a relay room.
*   **Attack Configuration:** Set up attacks remotely.
*   **Monitoring:** View real-time logs and progress.
