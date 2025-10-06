# Half-Hashed Kitty: Anywhere Access

![Alt text](app/src/main/res/drawable/half_hashed_kitty_banner.png?raw=true "Half-Hashed Kitty")

## Intro

Half-Hashed Kitty is a cross-platform tool for simplifying WiFi security auditing and password cracking. It consists of a powerful JavaFX desktop application, a lightweight Go-based relay server, and an Android mobile client.

This new architecture separates the user interface from the relay server, creating a more robust and modular system.

## The Architecture

The system has three main components that work in concert:

1.  **The Java Desktop Application (`hashkitty-java`):** The main control center. It provides the UI for managing `hashcat` attacks, remote sniffing, and application settings. It also launches and manages the Go relay process and acts as a client to it.
2.  **The Standalone Go Relay (`gokitty-relay`):** A high-performance, standalone WebSocket relay server. Its sole job is to pass messages between clients that have joined the same "room".
3.  **The Android App:** The mobile client used to connect to your desktop setup. It joins a specific room on the relay server to communicate with the desktop application.

---

## Setup and Installation

### Step 0: Prerequisites

Before you begin, make sure you have the following software installed:

-   **Java Development Kit (JDK):** Version 17 or higher.
-   **Go:** Version 1.18 or higher (required to build the relay).
-   **Hashcat:** Must be installed and accessible from your system's PATH. For GPU cracking, ensure you have the correct drivers installed (see the "Hashcat Setup" tab in the application for guidance).

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
Upon starting, the JavaFX application will launch the Go relay, connect to it, and display a QR code.

### Step 3: Set up the Android Application

1.  Open the project's root directory (`/app`) in Android Studio.
2.  Build and run the application on an Android device or an emulator.
3.  Scan the QR code displayed in the Java desktop application to connect.

---

## How to Use

### Local Attacks (Desktop App)

1.  Navigate to the **Attack** tab.
2.  Use the **"..."** buttons to select your **Hash File**, **Wordlist**, and an optional **Rule File**.
3.  Enter the appropriate **Hash Mode** for your hashes (e.g., `22000` for WPA2).
4.  Select your **Attack Mode** ("Dictionary" or "Mask").
5.  Click **"Start Local Attack"**. Monitor the status log for progress.

### Remote Attacks (Mobile App)

1.  Ensure the desktop application is running and you have connected the mobile app by scanning the QR code.
2.  From the mobile app, send an attack command with the hash string.
3.  The desktop application will execute the attack using a default dictionary and report the results back to the mobile client and the desktop UI.

### Hashcat Setup

If you are having trouble with `hashcat`, please consult the **Hashcat Setup** tab in both the desktop and Android applications for detailed instructions on installing the tool and the necessary GPU drivers.