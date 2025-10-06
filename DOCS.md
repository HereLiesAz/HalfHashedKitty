# Project Documentation

This document provides a detailed overview of the HashKitty project architecture, components, and communication flow.

## Project Overview

HashKitty is a cross-platform tool designed to provide a user-friendly interface for the powerful hash-cracking tool, `hashcat`. The project consists of three main components that work together:

1.  **Java Desktop Application (`hashkitty-java`):** The primary user interface and control center. It manages `hashcat` processes, remote sniffing sessions, and all application settings. It also launches the Go relay and connects to it as a client.
2.  **Standalone Go Relay (`gokitty-relay`):** A lightweight, high-performance WebSocket relay server written in Go. Its sole purpose is to relay messages between clients that have joined the same session "room".
3.  **Android Mobile Application (`app`):** A mobile client used to connect to the desktop application's session and send commands.

## Architecture & Communication Flow

The system's architecture ensures that the UI, relay logic, and cracking tools are all decoupled.

1.  **Startup:**
    -   The user launches the `hashkitty-java` application.
    -   The Java app immediately starts the `gokitty-relay` executable as a background process.
    -   The Java app generates a unique room ID (e.g., `a1b2c3d4`).
    -   The Java app creates an internal WebSocket client (`RelayClient`) and connects to its own local relay server at `ws://localhost:5001`.
    -   Upon connecting, the `RelayClient` sends a "join" message to the relay with its unique room ID.

2.  **Mobile Connection:**
    -   The Java app displays a QR code containing the server's local network IP address and the unique room ID (e.g., `ws://192.168.1.100:5001/ws?roomId=a1b2c3d4`).
    -   The user scans this QR code with the Android mobile app.
    -   The Android app connects to the `gokitty-relay` server and sends its own "join" message with the same room ID.
    -   Now, both the Java app and the Android app are in the same room on the relay server.

3.  **Task Execution (e.g., an Attack):**
    -   The user initiates an attack from the Android app.
    -   The Android app sends a JSON message of type "attack" to the relay server.
    -   The relay server receives the message and broadcasts it to all *other* clients in the room. In this case, it sends the message to the `RelayClient` inside the Java application.
    -   The `RelayClient` receives the "attack" message and triggers the `HashcatManager` to start the `hashcat` process.
    -   When `hashcat` cracks a password, the `HashcatManager` notifies the `App`, which in turn tells the `RelayClient` to send a "cracked" message back through the relay, which is then received by the Android app.

## Setup and Running

The setup is a two-step process:

1.  **Build the Go Relay:**
    Compile the standalone relay server. The resulting executable must be placed inside the `/app/hashkitty-java/` directory.
    ```bash
    # Navigate to the Go relay directory
    cd /app/gokitty-relay

    # Build and place the executable
    go build -o ../hashkitty-java/gokitty-relay ./cmd/gokitty-relay
    ```

2.  **Run the Java Desktop App:**
    With the relay executable in place, run the main application.
    ```bash
    # Navigate to the Java application directory
    cd /app/hashkitty-java

    # Run the application using the Gradle wrapper
    ./gradlew run
    ```