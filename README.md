# Half-Hashed Kitty: Anywhere Access

![Alt text](app/src/main/res/drawable/half_hashed_kitty_banner.png?raw=true "Half-Hashed Kitty")

## Intro

Half-Hashed Kitty is a cross-platform tool for simplifying WiFi security auditing and password cracking. It consists of a powerful JavaFX desktop application, a lightweight Go-based relay server, and an Android mobile client.

This new architecture separates the user interface from the relay server, creating a more robust and modular system.

## The Architecture

The system has three main components that work in concert:

1.  **The Java Desktop Application (`hashkitty-java`):** The main control center. It provides the UI for managing `hashcat` attacks, remote sniffing, application settings, and connecting to Hashtopolis. It also launches and manages the Go relay process and acts as a client to it.
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

### Connecting to the Relay

By default, the desktop application starts its own local relay server. Your mobile app can connect to this by scanning the QR code.

If you are running the `gokitty-relay` server on a different machine (e.g., a Raspberry Pi or a cloud server), you can connect to it manually:
1.  In the desktop app, find the "Mobile Connection" box.
2.  Enter the full WebSocket address of your remote relay (e.g., `ws://192.168.1.50:5001`).
3.  Click **"Connect"**. The QR code will update to point to your remote relay.

### Running an Attack

1.  Navigate to the **Attack** tab.
2.  Fill in the **Hash File** and **Hash Mode**.
3.  For a **Dictionary Attack**, you can either:
    *   Click the **"..."** button to select a **local wordlist file**.
    *   Or, paste a URL into the **Wordlist URL** field to download a wordlist from the web.
4.  For a **Mask Attack**, enter your mask. You can use the helper buttons (`?l`, `?u`, `?d`, etc.) to build the mask string.
5.  Click **"Start Local Attack"**. The configuration options will be disabled while the attack is running. Monitor the status log for progress.

### Remote Attacks (Mobile App)

1.  Ensure the desktop application is running and you have connected the mobile app by scanning the QR code.
2.  From the mobile app, send an attack command with the hash string.
3.  The desktop application will execute the attack using a default dictionary and report the results back to the mobile client and the desktop UI.

### Hashcat Setup

If you are having trouble with `hashcat`, please consult the **Hashcat Setup** tab in both the desktop and Android applications for detailed instructions on installing the tool and the necessary GPU drivers.