# Half-Hashed Kitty: Anywhere Access

![Alt text](app/src/main/res/drawable/half_hashed_kitty_banner.png?raw=true "Half-Hashed Kitty")

## Intro

Half-Hashed Kitty is a cross-platform tool for simplifying WiFi security auditing and password cracking. It consists of a powerful JavaFX desktop application, a lightweight Go-based relay server, and an Android mobile client.

This new architecture separates the user interface from the relay server, creating a more robust and modular system.

## The Architecture

The system has three main components that work in concert:

1.  **The Java Desktop Application (`hashkitty-java`):** The main control center. It provides a rich UI for managing `hashcat` attacks with advanced options, remote sniffing, application settings, and connecting to Hashtopolis. It also features an automated setup process to help install dependencies. The app can launch and manage the Go relay process or connect to a remote one.
2.  **The Standalone Go Relay (`gokitty-relay`):** A high-performance, standalone WebSocket relay server. Its sole job is to pass messages between clients that have joined the same "room".
3.  **The Android App:** The mobile client used to connect to your desktop setup. It joins a specific room on the relay server to communicate with the desktop application.

---

## Setup and Installation

The desktop application features an automated setup process to simplify dependency installation.

### Step 1: Automated Setup (Desktop App)

1.  Run the Java desktop application: `cd /app/hashkitty-java && ./gradlew run`
2.  Navigate to the **Setup** tab.
3.  Click **"Download & Unpack Hashcat"**. This will automatically download the latest version of Hashcat and unpack it into a `hashcat` directory in your user's home folder. The application will provide the full path upon completion.
4.  **Add Hashcat to your system's PATH.** This is a manual step that is required for the application to find the `hashcat` executable.
5.  Use the other buttons on the Setup screen to open the official download pages for your GPU drivers (NVIDIA, AMD, or Intel). Driver installation must be done manually to ensure you select the correct version for your specific hardware.

### Step 2: Build the Go Relay (Optional)

If you plan to use the relay server on a machine other than your local desktop, you will need to build it from source.

```bash
# Navigate to the Go relay directory
cd /app/gokitty-relay
# Build the relay
go build ./cmd/gokitty-relay
```

### Step 3: Set up the Android Application

1.  Open the project's root directory (`/app`) in Android Studio.
2.  Build and run the application on an Android device or an emulator.

---

## How to Use

### Connecting the Mobile App

By default, the desktop application starts its own local relay server. Your mobile app can connect to this by scanning the QR code in the "Mobile Connection" box.

You can also connect to a remote relay server or use a direct connection for local networks using the options in the "Mobile Connection" box.

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