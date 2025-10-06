# Project Documentation

This document provides an overview of the HashKitty project architecture, components, and setup instructions.

## Project Overview

HashKitty is a cross-platform tool designed to provide a user-friendly interface for the powerful hash-cracking tool, `hashcat`. The project consists of two main components:

1.  **Java Desktop Application:** A JavaFX-based application that serves as the main hub. It runs a relay server, manages `hashcat` processes, and provides a user interface for monitoring and control.
2.  **Android Mobile Application:** A mobile client that allows users to connect to the desktop application, submit hashes for cracking, and view results remotely.

## Architecture

The system is designed around a client-server model facilitated by a WebSocket relay.

-   **Relay Server:** The Java desktop application includes a built-in WebSocket relay server. This server creates "rooms" based on a unique ID, allowing multiple clients (e.g., the desktop UI and one or more mobile apps) to join the same session and communicate in real-time.
-   **Communication Protocol:** Clients communicate by sending JSON-formatted messages over the WebSocket connection. Key message types include `join` (to enter a room) and `attack` (to submit a hash for cracking).
-   **Hashcat Integration:** The Java application manages `hashcat` as a separate command-line process. It constructs the appropriate commands, starts the process, and monitors its output for results.

## Setup and Running the Application

### Prerequisites

-   **Java Development Kit (JDK):** Version 17 or higher.
-   **Hashcat:** Must be installed and available in the system's PATH.
-   **Gradle:** Used for building the Java application.

### Building and Running the Java Desktop App

1.  Navigate to the `/app/hashkitty-java` directory.
2.  Build the application using Gradle:
    ```bash
    gradle build
    ```
3.  Run the application:
    ```bash
    gradle run
    ```
The application will start, display a QR code for the relay server, and be ready to accept connections from the mobile client.

### Building the Android App

1.  Open the `/app` directory in Android Studio.
2.  Let Gradle sync the project.
3.  Build and run the application on an Android device or emulator.
4.  Use the "Connect" feature in the app to scan the QR code displayed on the desktop application.