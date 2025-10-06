# Half-Hashed Kitty: Project Documentation

This document provides a comprehensive overview of the Half-Hashed Kitty project, including its architecture, setup, and usage.

## 1. Project Overview

Half-Hashed Kitty is a remote management tool for Hashcat, allowing you to run and monitor WiFi security audits from an Android device. The system consists of two main components:

-   **`gokitty` (Go Desktop Server):** A lightweight, cross-platform server that runs on your desktop and manages Hashcat processes.
-   **Android App:** A mobile application that provides a user-friendly interface for controlling the desktop server.

The communication between the two is facilitated by a WebSocket-based protocol, which can be run in either a direct local connection mode or through a relay server for remote access.

## 2. System Architecture

The project's architecture is designed for simplicity and robustness.

### 2.1. `gokitty` Go Desktop Server

The Go server has two modes of operation:

-   **Client Mode:** This is the default mode. The server starts, connects to a WebSocket relay, and generates a unique Room ID. It then displays a QR code containing this ID, which can be scanned by the Android app to establish a connection. In this mode, the server listens for commands from the app (e.g., to start an attack) and streams back the output from Hashcat. It also handles the automatic download and installation of Hashcat if it's not already present.
-   **Relay Mode:** In this mode, the `gokitty` server acts as a simple WebSocket message broker. It allows multiple clients (both desktop and mobile) to connect to the same "room" and exchange messages, enabling communication even if they are not on the same local network.

### 2.2. Android Application

The Android app is the control center for the system. It allows you to:

-   **Connect to the Desktop Server:** By scanning a QR code or manually entering a Room ID.
-   **Configure and Start Attacks:** Specify the target hash file, attack mode, wordlists, and rules.
-   **Monitor Progress:** View the real-time terminal output from Hashcat.

## 3. Communication Protocol

Communication is handled via a JSON-based WebSocket protocol. All messages share a common structure:

```json
{
  "type": "message_type",
  "room_id": "session_room_id",
  "payload": { ... }
}
```

### Key Message Types:

-   `join`: Sent by the desktop client to the relay server to create or join a room.
-   `attack`: Sent by the mobile app to the desktop client to start a new Hashcat job.
-   `status_update`: Sent by the desktop client to the mobile app to provide updates on the attack's progress, including terminal output and error messages.

## 4. Setup and Installation

### Prerequisites

-   **Go:** For building the desktop server.
-   **Android Studio:** For building the Android app.

### 4.1. Building and Running the `gokitty` Server

1.  **Navigate to the `gokitty` directory:**
    ```bash
    cd /app/gokitty
    ```

2.  **Build the executable:**
    ```bash
    go build .
    ```

3.  **Run in Client Mode (to be controlled by the app):**
    ```bash
    # This will connect to a local relay by default
    ./gokitty --mode=client

    # Or connect to a remote relay
    ./gokitty --mode=client --relay=ws://your-relay-server.com/ws
    ```
    When you run the client, it will display a QR code in your terminal.

4.  **Run in Relay Mode (to allow remote connections):**
    ```bash
    ./gokitty --mode=relay
    ```
    This will start a WebSocket server on port `5001`.

### 4.2. Building and Running the Android App

1.  **Open the project** in Android Studio (the root of the `/app` directory).
2.  **Build and run** the app on an Android device or emulator.
3.  **Connect to the server:**
    -   Ensure your Android device is on the same network as the server (if not using a public relay).
    -   Scan the QR code displayed by the `gokitty` client.

## 5. KDoc and Code Documentation

This section provides an overview of the key classes and their responsibilities.

### 5.1. `gokitty` (Go)

-   `main.go`: The entry point for the application. Handles command-line arguments and starts the server in either client or relay mode.
-   `runDesktopClient()`: Contains the logic for the client mode, including connecting to the relay, generating QR codes, and handling incoming messages.
-   `runRelayServer()`: Implements the WebSocket relay server.
-   `runAttack()`: Manages the execution of Hashcat, including input validation and streaming output back to the mobile app.
-   `prereqSetup()`: Handles the automatic download and extraction of Hashcat.

### 5.2. Android App (Kotlin)

-   `MainActivity.kt`: The main entry point for the Android app. Hosts the Jetpack Compose UI.
-   `MainViewModel.kt`: The primary ViewModel for the app. It manages the WebSocket connection, sends commands to the server, and holds the UI state.
-   `MainScreen.kt`: Defines the main UI of the app using Jetpack Compose, including the input fields, attack button, and terminal view.
-   `QrCodeAnalyzer.kt`: Implements the QR code scanning functionality using CameraX.

This documentation serves as a starting point. For more detailed information, refer to the source code and the `TODO.md` file for planned improvements.