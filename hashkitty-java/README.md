# Half-Hashed Kitty - Java Desktop Application

The `hashkitty-java` module contains the desktop application for Half-Hashed Kitty. It is built with JavaFX and acts as the central hub for managing attacks, sniffing, and relaying commands from the Android client.

## Prerequisites

*   Java JDK 17 or higher.
*   Hashcat installed and accessible in the system PATH (or configured within the app).
*   Correct GPU drivers for Hashcat.

## Building

This project uses Gradle.

```bash
# Build the project
./gradlew clean build

# Run unit tests
./gradlew test
```

## Running

```bash
# Run the application
./gradlew run
```

## Structure

*   `src/main/java/hashkitty/java/`: Source code.
    *   `App.java`: Main entry point.
    *   `hashcat/`: Hashcat process management.
    *   `server/`: Embedded Relay Server.
    *   `relay/`: Client for connecting to relays (self or remote).
*   `src/main/resources/`: FXML files, CSS, and images.

## Features

*   **Attack Management:** Configure and run Dictionary and Mask attacks.
*   **Relay Server:** Built-in WebSocket server for Android connection.
*   **Hashtopolis Integration:** Connect to Hashtopolis agents.
*   **Sniffing:** Remote packet capture via SSH/TShark.
