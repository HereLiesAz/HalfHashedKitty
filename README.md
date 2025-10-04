# Half-Hashed Kitty: Anywhere Access

![Alt text](app/src/main/res/drawable/half_hashed_kitty_banner.png?raw=true "Half-Hashed Kitty")

## Intro

Half-Hashed Kitty is a project that consists of an Android application and a desktop application for WiFi security auditing. This new version features a simple, robust, and self-contained desktop application written in Go that allows you to run and monitor Hashcat jobs from your Android phone.

## The Architecture

The system uses a simple and reliable two-part architecture:

1.  **The Go Application (`gokitty`):** A single, self-contained executable that runs on your desktop computer. It requires no external dependencies. It can run in two modes:
    *   **Client Mode:** Connects to a relay server, displays a QR code with a unique room ID, and executes Hashcat commands received from the Android app.
    *   **Relay Mode:** Acts as a WebSocket relay server, allowing the desktop and mobile apps to communicate even if they are not on the same network.
2.  **The Android App:** The mobile app that allows you to control the desktop server. You connect it to your desktop by scanning the QR code, which establishes a connection through the relay server.

---

## Setup and Installation

### Step 0: Prerequisites

Before you begin, make sure you have the following software installed on your computer:

-   **Go:** [Download Go](https://go.dev/dl/) (for building the server)
-   **Android Studio:** [Download Android Studio](https://developer.android.com/studio) (for building the app)

### Step 1: Build the Go Application

The desktop application is a single, self-contained executable.

**Instructions (for any OS):**
```bash
# Navigate to the Go application directory
cd gokitty

# Tidy dependencies
go mod tidy

# Build the executable
go build ./cmd/gokitty
```
This will create a `gokitty` (or `gokitty.exe` on Windows) executable in the `gokitty` directory.

### Step 2: Run the Desktop Application

To run the application, you have two options:

**A) Run a Local Relay and Client:**
If you want to run everything on your local network, you'll need two terminals.

*   **Terminal 1: Start the Relay Server**
    ```bash
    ./gokitty --mode=relay
    ```
*   **Terminal 2: Start the Desktop Client**
    ```bash
    ./gokitty --mode=client
    ```

**B) Run Only the Client (using a public relay):**
If you are using a publicly hosted relay server, you only need to run the client.
```bash
./gokitty --mode=client --relay=ws://your-public-relay.com/ws
```

Upon starting the client, it will display a **QR code** in the terminal. You will scan this QR code with the Android app to pair the devices.

### Step 3: Set up the Android Application

**Building the App:**
1. Open the project in Android Studio (the `/app` directory).
2. Build and run it on your Android device.

**Connecting the App:**
1. Open the app and navigate to the connection screen.
2. Scan the QR code displayed in the terminal by the desktop client.
3. The app will now be connected to your desktop.

## How to Use

1.  Ensure the Go desktop client is running and you have scanned the QR code with the Android app.
2.  Navigate to the **Input** tab in the app to specify the hash file path and other parameters. **Note:** The file paths must be valid on the desktop computer where the server is running.
3.  Go to the **Attack** tab and press "Start Remote Attack".
4.  You can monitor the progress and see the final results in the **Terminal** tab in the app.