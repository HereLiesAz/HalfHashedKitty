# Half-Hashed Kitty

![Alt text](app/src/main/res/drawable/half_hashed_kitty_banner.png?raw=true "Half-Hashed Kitty")

## Intro

Half-Hashed Kitty is a project that consists of an Android application and a desktop application for WiFi security auditing.

This project is under active development. If you spot any bugs, please submit an issue with as much detail as possible.

## Desktop Application

The desktop application is a Java application that acts as a connection manager for the Android application.

### Building the Desktop Application

To build the desktop application, you need to have Java 8 or newer and Maven installed.

Navigate to the `desktop-app` directory and run the following command:
```
mvn clean package
```
This will create a fat jar in the `target` directory named `connection-manager-1.0-SNAPSHOT.jar`.

### Running the Desktop Application

To run the desktop application, you can use the provided wrapper scripts in the `desktop-app` directory:
-   `run.sh` for Linux and Mac.
-   `run.bat` for Windows.

Alternatively, you can run the fat jar directly from the command line:
```
java -jar desktop-app/target/connection-manager-1.0-SNAPSHOT.jar
```

### Desktop Application UI

The desktop application has a single screen with a QR code and an input field for an API key.

-   **QR Code**: Scan this QR code with the Half-Hashed Kitty Android app to connect to this desktop application.
-   **API Key**: Enter the API key from the Hashtopolis web interface.

## Android Application

The Android application provides a user interface for various WiFi security auditing tools.

### Building the Android Application

To build the Android application, you need to have Android Studio installed. Open the project in Android Studio and build it.

### Android Application UI

The Android application has a tabbed interface with the following tabs:

-   **Input**: This tab is for providing the input for the hash cracking process. You can either enter the hash directly, or upload a ZIP or PCAPNG file to extract the hash from it.
-   **Attack**: This tab is for starting the hash cracking attack on the remote server.
-   **Wordlist**: This tab is for specifying the path to the wordlist file on the remote server.
-   **Mask**: This tab is for creating and selecting masks for hash cracking attacks.
-   **Capture**: This tab is for capturing wireless network packets to get the handshake for hash cracking.
-   **Terminal**: This tab shows the raw output from the tools that are being run.
-   **Output**: This tab will show the cracked password once it has been found.
-   **Hashtopolis**: This tab is for connecting to a Hashtopolis server to manage your hash cracking agents.
-   **Pi Control**: This tab is for connecting to a Raspberry Pi that is running the necessary tools.
