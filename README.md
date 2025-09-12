# Half-Hashed Kitty

![Alt text](app/src/main/res/drawable/half_hashed_kitty_banner.png?raw=true "Half-Hashed Kitty")

## Intro

Half-Hashed Kitty is a project that consists of an Android application and a desktop application for WiFi security auditing.

This project is under active development. If you spot any bugs, please submit an issue with as much detail as possible.

## Desktop Application

The desktop application is a Java application that acts as a connection manager for the Android application and a client for a Hashtopolis server.

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

### Connecting to a Hashtopolis Server

The desktop application acts as a client for a [Hashtopolis](https://hashtopolis.org/) server. You need to have a Hashtopolis server running to use the task management features of the desktop app.

You can either install Hashtopolis manually on your local machine or a remote server.

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

To build the Android application, you need to have Android Studio installed. Open the project in Android Studio and build it.

### Android Application UI

The Android application has a tabbed interface. Here is a description of each tab and its UI elements:

-   **Setup**: This tab provides a detailed guide on how to set up a remote hashcat server. This is a crucial first step to use the app.
-   **PC Connect**: This tab is used to connect to the desktop application. It opens a camera preview to scan the QR code from the PC app.
-   **Input**: This tab is for providing the input for the hash cracking process.
    -   **Server URL**: A text field to enter the URL of your remote hashcat server.
    -   **Enter hash**: A text field to enter the hash you want to crack.
    -   **Detect Hash**: A button to automatically identify the hash type.
    -   **Upload Zip**: A button to upload a ZIP file containing a hash.
    -   **Upload PCAPNG**: A button to upload a PCAPNG file to extract a hash.
    -   **Hash Mode**: A dropdown to select the hash mode.
-   **Command Builder**: This tab is for configuring the attack command.
    -   **Attack Mode**: A dropdown to select the attack mode.
    -   **Rules File**: A text field for the rules file.
    -   **Custom Mask**: A text field for a custom mask.
    -   **Force**: A checkbox to force the attack.
-   **Wordlist**: This tab is for specifying the path to the wordlist file on the remote server.
    -   **Remote Wordlist Path**: A text field to enter the path to your wordlist file.
-   **Mask**: This tab is for creating and selecting masks for hash cracking attacks.
    -   **Create Mask**: A button to create a new mask.
    -   **Select Mask File**: A button to select a mask file.
-   **Attack**: This tab is for starting the hash cracking attack on the remote server.
    -   **Start Remote Attack**: A button to start the attack.
-   **Capture**: This tab is for capturing wireless network packets to get the handshake for hash cracking.
    -   **Start/Stop Capture**: A button to start or stop the packet capture.
    -   **Live Output**: A text area that shows the live output from the capture process.
-   **Terminal**: This tab shows the raw output from the tools that are being run. It provides a terminal-like view of the process.
-   **Output**: This tab will show the cracked password once it has been found.
-   **Hashtopolis**: This tab is for connecting to a Hashtopolis server to manage your hash cracking agents.
    -   **Hashtopolis Server URL**: A text field for the server URL.
    -   **API Key**: A text field for the API key.
    -   **Get Agents**: A button to fetch and display the list of agents.
    -   **Agent List**: A list of agents with their status and last activity.
-   **Pi Control**: This tab is for connecting to a Raspberry Pi that is running the necessary tools. It opens a camera preview to scan a QR code.
