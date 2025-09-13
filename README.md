# Half-Hashed Kitty

![Half-Hashed Kitty Banner](app/src/main/res/drawable/half_hashed_kitty_banner.png?raw=true "Half-Hashed Kitty")

A cross-platform suite for WiFi security auditing and hash cracking, featuring a powerful desktop connection manager and a versatile Android application.

## Table of Contents

- [Introduction](#introduction)
- [Project Structure](#project-structure)
- [Desktop Application](#desktop-application)
  - [Building the Desktop App](#building-the-desktop-application)
  - [Running the Desktop App](#running-the-desktop-application)
  - [Connecting to Hashtopolis](#connecting-to-a-hashtopolis-server)
  - [Desktop App UI](#desktop-application-ui)
- [Android Application](#android-application)
  - [Building the Android App](#building-the-android-application)
  - [Android App UI](#android-application-ui)
- [Contributing](#contributing)
- [License](#license)

## Introduction

Half-Hashed Kitty is a project that consists of an Android application and a desktop application for WiFi security auditing. It provides a graphical user interface for popular tools and integrates with [Hashtopolis](https://hashtopolis.org/) for distributed hash cracking.

This project is under active development. If you spot any bugs, please [submit an issue](https://github.com/your-repo/issues) with as much detail as possible.

## Project Structure

The repository is organized as follows:

-   `app/`: Contains the source code for the Android application.
-   `desktop-app/`: Contains the source code for the Java-based desktop connection manager.
-   `src/`: Contains legacy source code for the original GUI and CLI.
-   `scripts/`: Contains various helper scripts for interacting with hashcat.
-   `*.sh`: Root-level shell scripts for managing hashcat modes.

## Desktop Application

The desktop application is a Java application that acts as a connection manager for the Android application and a client for a Hashtopolis server.

### Building the Desktop Application

To build the desktop application, you need to have Java 8 or newer and Maven installed.

Navigate to the `desktop-app` directory and run the following command:
```bash
mvn clean package
```
This will create a fat jar in the `target` directory named `connection-manager-1.0-SNAPSHOT.jar`.

### Running the Desktop Application

To run the desktop application, you can use the provided wrapper scripts in the `desktop-app` directory:
-   `run.sh` for Linux and macOS.
-   `run.bat` for Windows.

Alternatively, you can run the fat jar directly from the command line:
```bash
java -jar desktop-app/target/connection-manager-1.0-SNAPSHOT.jar
```

### Connecting to a Hashtopolis Server

The desktop application acts as a client for a [Hashtopolis](https://hashtopolis.org/) server. You need to have a Hashtopolis server running to use the task management features of the desktop app.

Once you have a running Hashtopolis server, you can connect the desktop application to it using the "Connection" tab.

### Desktop Application UI

The desktop application has a tabbed interface and a dark theme.

-   **Connection Tab**: This tab is for connecting to the Android app and the Hashtopolis server.
    -   **QR Code**: Scan this QR code with the Android app to connect it to this desktop application.
    -   **Server URL**: Enter the URL of your Hashtopolis server (e.g., `http://localhost:8080`).
    -   **API Key**: Enter the API key from your Hashtopolis web interface.
    -   **Save Settings Button**: Saves the Server URL and API Key.
    -   **Connect Button**: Connects to the Hashtopolis server and opens the "Tasks" tab.
-   **Tasks Tab**: This tab displays the list of tasks from the Hashtopolis server. It is enabled after a successful connection.

## Android Application

The Android application provides a user interface for various WiFi security auditing tools.

### Building the Android Application

To build the Android application, you need to have Android Studio installed. Open the project in Android Studio and let it sync with Gradle, then build the project.

### Android Application UI

The Android application has a tabbed interface. Here is a description of each tab:

-   **Setup**: A detailed guide on how to set up a remote hashcat server.
-   **PC Connect**: Connects to the desktop application by scanning its QR code.
-   **Input**: Provides various ways to input hashes (manual entry, ZIP, PCAPNG) and detect hash types.
-   **Command Builder**: Configures the attack command with modes, rules, and masks.
-   **Wordlist**: Specifies the path to the wordlist file on the remote server.
-   **Mask**: Creates and selects masks for hash cracking attacks.
-   **Attack**: Starts the hash cracking attack on the remote server.
-   **Capture**: Captures wireless network packets to get the handshake for hash cracking.
-   **Terminal**: Shows the raw output from the tools that are being run.
-   **Output**: Shows the cracked password once found.
-   **Hashtopolis**: Manages hash cracking agents by connecting to a Hashtopolis server.
-   **Pi Control**: Connects to a Raspberry Pi running the necessary tools by scanning a QR code.

## Contributing

Contributions are welcome! If you'd like to contribute, please fork the repository and create a pull request. For major changes, please open an issue first to discuss what you would like to change.

## License

This project is licensed under the [MIT License](LICENSE).
