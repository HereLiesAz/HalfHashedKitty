# Half-Hashed Kitty: Anywhere Access

![Alt text](app/src/main/res/drawable/half_hashed_kitty_banner.png?raw=true "Half-Hashed Kitty")

## Intro

Half-Hashed Kitty is a cross-platform tool for simplifying WiFi security auditing and password cracking. It consists of a powerful JavaFX desktop application, a lightweight Go-based relay server, and an Android mobile client.

This new architecture separates the user interface from the relay server, creating a more robust and modular system.

## The Architecture

The system has three main components that work in concert:

1.  **The Java Desktop Application (`hashkitty-java`):** The main control center. It provides the UI for managing `hashcat` attacks, remote sniffing, and settings. It also launches and manages the Go relay process and acts as a client to it.
2.  **The Standalone Go Relay (`gokitty-relay`):** A high-performance, standalone WebSocket relay server. Its sole job is to pass messages between clients that have joined the same "room".
3.  **The Android App:** The mobile client used to connect to your desktop setup. It joins a specific room on the relay server to communicate with the desktop application.

---

## Setup and Installation

### Step 0: Prerequisites

Before you begin, make sure you have the following software installed:

-   **Java Development Kit (JDK):** Version 17 or higher.
-   **Go:** Version 1.18 or higher (required to build the relay).
-   **Hashcat:** Must be installed and accessible from your system's PATH.

### Step 1: Build the Standalone Go Relay

First, you need to compile the relay server. The output executable must be placed in the `hashkitty-java` directory.

```bash
# Navigate to the Go relay directory
cd /app/gokitty-relay

# Build the relay and place it in the Java app's directory
# (Adjust the output name if you are on Windows, e.g., gokitty-relay.exe)
go build -o ../hashkitty-java/gokitty-relay ./cmd/gokitty-relay
```
For more detailed cross-compilation instructions (e.g., for Raspberry Pi), see `gokitty-relay/BUILD.md`.

### Step 2: Build and Run the Java Application

With the relay executable in place, you can now run the main desktop application using the Gradle wrapper.

```bash
# Navigate to the Java application directory
cd /app/hashkitty-java

# Use the Gradle wrapper to run the application
./gradlew run
```
Upon starting, the JavaFX application will:
1.  Launch the Go relay server in the background.
2.  Connect to the relay as a client, creating a unique session "room".
3.  Display a QR code containing the address of the relay and the unique room ID.

### Step 3: Set up the Android Application

**Building the App:**
1. Open the project's root directory (`/app`) in Android Studio.
2. Allow Android Studio to sync the Gradle project.
3. Build and run the application on your Android device or an emulator.

**Connecting the App:**
1. Open the app and navigate to the **Connect** screen.
2. Scan the QR code displayed in the Java desktop application. This will give the mobile app the address and room ID it needs to connect to the relay and communicate with your desktop.
3. Once connected, you can send attack commands from the mobile app to be executed by the desktop application.