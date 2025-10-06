# Half-Hashed Kitty: Anywhere Access

![Alt text](app/src/main/res/drawable/half_hashed_kitty_banner.png?raw=true "Half-Hashed Kitty")

## Intro

Half-Hashed Kitty is a project that consists of an Android application and a Java-based desktop application for simplifying WiFi security auditing. This new version replaces the previous Go implementation with a more feature-rich JavaFX application that includes a built-in relay server, a graphical user interface, and direct integration with `hashcat`.

## The Architecture

The system is designed for ease of use, with two core components:

1.  **The Java Desktop Application (`hashkitty-java`):** A graphical desktop application built with JavaFX. It runs a WebSocket relay server, manages `hashcat` cracking sessions, and displays a QR code for easy mobile client connection. It is the central hub for all operations.
2.  **The Android App:** The mobile client for controlling the desktop application. You connect it to your desktop by scanning the QR code, which establishes a real-time connection. From the app, you can submit hashes and monitor the cracking process.

---

## Setup and Installation

### Step 0: Prerequisites

Before you begin, make sure you have the following software installed on your computer:

-   **Java Development Kit (JDK):** Version 17 or higher.
-   **Hashcat:** Must be installed and accessible from your system's PATH.
-   **Gradle:** The project uses the Gradle wrapper, so no separate installation is required.

### Step 1: Build and Run the Java Application

The desktop application is built and run using the included Gradle wrapper.

**Instructions (for any OS):**
```bash
# Navigate to the Java application directory
cd /app/hashkitty-java

# Use the Gradle wrapper to run the application
# This will automatically download dependencies and build the project
./gradlew run
```
Upon starting, the JavaFX application window will appear, displaying a **QR code**. You will scan this with the Android app to pair the devices. The window also provides a status log and a field for displaying cracked passwords.

### Step 2: Set up the Android Application

**Building the App:**
1. Open the project's root directory (`/app`) in Android Studio.
2. Allow Android Studio to sync the Gradle project.
3. Build and run the application on your Android device or an emulator.

**Connecting the App:**
1. Open the app and navigate to the **Connect** screen.
2. Scan the QR code displayed in the Java desktop application.
3. The app will connect to your desktop, and you can begin submitting hashes.

## How to Use

1.  Ensure the Java desktop application is running.
2.  Scan the QR code with the Android app to establish a connection.
3.  From the Android app, send an "attack" command with the necessary hash information.
4.  Monitor the status log in the desktop application to see real-time updates from the relay server and the `hashcat` process.
5.  Any cracked passwords will be displayed in the desktop UI and sent back to the mobile client.