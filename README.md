# Half-Hashed Kitty

*[app/main/src/res/drawable/halfhashedkitty_banner.png]*

## Intro

Half-Hashed Kitty is an Android application that provides a Graphical User Interface for various WiFi security auditing tools, including the `aircrack-ng` suite and online hash-cracking utilities. It allows users to both process existing capture files and perform live, on-device packet capturing on supported hardware.

This project is under active development. If you spot any bugs, please submit an issue with as much detail as possible.

## Features

- **File-based Handshake Extraction**:
  - Upload `.pcapng` capture files directly within the app.
  - Automatically sends the capture to the `hashcat.net/cap2hashcat` online service to extract the WPA/WPA2 handshake.
  - The extracted hash is loaded into the app for further actions.

- **On-Device Packet Capture (Root Required)**:
  - Perform live WPA/WPA2 handshake captures.
  - Automatically checks for root access and handles the installation of necessary command-line tools.
  - Dynamically detects the device's CPU architecture and wireless interface (`wlanX`) to ensure compatibility.
  - Provides a "Capture" tab with a terminal-like view for real-time output from the capture process.
  - **Note**: This feature requires a rooted device with a wireless chipset that supports monitor mode.

- **Hashcat Integration**:
  - A full GUI for managing hashcat and hashtopolis tasks.
  - Auto-detection of hash types.
  - Support for various attack modes, including dictionary and mask attacks.

## Requirements

1.  **Android Version**: Android 8.0 (Oreo) or newer.
2.  **Root Access**: Required for the on-device packet capturing feature. The app will request `su` permissions when the capture is started.
3.  **Monitor Mode Interface**: For on-device capturing, a wireless chipset that supports monitor mode is essential. This may require custom firmware (e.g., Nexmon) or an external USB WiFi adapter that is compatible with Android.

## Roadmap

### Desktop App: Integrate Hashcat and Hashtopolis (with Docker)

- Set up hashtopolis and hashcat using Docker.
- Implement UI in the desktop app to manage the Docker containers (e.g., start/stop buttons).
- Create a Java client to interact with the Hashtopolis API.
- Design and implement a UI in the desktop app for managing hashcat tasks (e.g., creating tasks, viewing results) via the Hashtopolis API.

### Android & Desktop App: Finalize Connection and UI

- Implement the WebSocket client in the Android app to receive real-time updates from the desktop app.
- Create a UI in the Android app to display the status and results of hashcat/hashtopolis tasks.

### Final Testing and Verification

- Thoroughly test the entire workflow:
  - Android app UI and navigation.
  - Root command execution.
  - QR code scanning and connection.
  - Desktop app functionality.
  - Docker container management.
  - WebSocket communication.
  - Task management via the Hashtopolis API.
