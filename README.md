# Half-Hashed Kitty: Anywhere Access

![Alt text](app/src/main/res/drawable/half_hashed_kitty_banner.png?raw=true "Half-Hashed Kitty")

## Intro

Half-Hashed Kitty is a cross-platform tool for simplifying WiFi security auditing and password cracking. It consists of a powerful JavaFX desktop application, a Java-based relay server, and an Android mobile client.

This architecture separates the user interface from the relay server, creating a more robust and modular system.

## The Architecture

The system has three main components that work in concert:

1.  **The Java Desktop Application (`hashkitty-java`):** The main control center. It provides a rich UI for managing `hashcat` attacks with advanced options, remote sniffing, application settings, and connecting to Hashtopolis. It hosts the Relay Server internally.
2.  **The Java Relay Server:** A WebSocket relay server now embedded directly within the Java Desktop Application (replacing the older Go version). Its sole job is to pass messages between clients that have joined the same "room".
3.  **The Android App:** The mobile client used to connect to your desktop setup. It joins a specific room on the relay server to communicate with the desktop application.

---

## Setup and Installation

### Step 1: Desktop App Setup

1.  Navigate to the desktop app directory: `cd hashkitty-java`
2.  Build the application: `./gradlew clean build`
3.  Run the application: `./gradlew run`

The desktop app will automatically start the embedded Relay Server on port 5001.

### Step 2: Hashcat Setup

1.  In the Desktop App, navigate to the **Hashcat Setup** tab.
2.  Click **"Download & Unpack Hashcat"**. This will automatically download the latest version of Hashcat and unpack it into a `hashcat` directory in your user's home folder.
3.  **Add Hashcat to your system's PATH** or ensure the app can find it.
4.  Use the other buttons on the Setup screen to open the official download pages for your GPU drivers (NVIDIA, AMD, or Intel).

### Step 3: Android App Setup

1.  Open the project's root directory in Android Studio.
2.  Build and run the application on an Android device or an emulator.
3.  Alternatively, build via command line: `./gradlew :app:assembleDebug`

---

## How to Use

### Connecting the Mobile App

By default, the desktop application starts its own local relay server. Your mobile app can connect to this by scanning the QR code in the "Mobile Connection" box on the Desktop App's main screen.

### Running an Attack

1.  Navigate to the **Attack** tab.
2.  Fill in the **Hash File** and **Hash Mode**.
3.  For a **Dictionary Attack**, you can either use a local wordlist file or paste a URL to a wordlist.
4.  For a **Mask Attack**, use the helper buttons (`?l`, `?u`, `?d`, etc.) to build the mask string.
5.  Configure **Advanced Options** like `--force`, optimized kernels (`-O`), and workload profile (`-w`).
6.  Click **"Start Local Attack"**. The configuration options will be disabled while the attack is running.

### Other Features

-   **Hashtopolis:** Connect to a Hashtopolis server to view and manage tasks.
-   **Sniff:** Remotely capture packets from a configured device (e.g., a Raspberry Pi).
-   **Learn:** Read beginner-friendly explanations of Hashcat and Hashtopolis.
-   **Settings:** Manage saved remote connections and import/export configurations.

## Documentation

See the `docs/` folder for comprehensive documentation on:
- [Architecture](ARCHITECTURE.md)
- [Screens](docs/screens.md)
- [Workflow](docs/workflow.md)
