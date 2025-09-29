# Half-Hashed Kitty: Anywhere Access

![Alt text](app/src/main/res/drawable/half_hashed_kitty_banner.png?raw=true "Half-Hashed Kitty")

## Intro

Half-Hashed Kitty is a project that consists of an Android application and a desktop application for WiFi security auditing. This version has been completely redesigned to allow you to run and monitor Hashcat jobs on your desktop computer from your Android phone, anywhere in the world.

The system uses a secure, three-part architecture to provide a seamless and persistent connection that is not limited to your local network.

## The "Anywhere Access" Architecture

The system is composed of three components that work together:

1.  **The Relay Server:** A lightweight Python server that acts as a secure middleman. It lives on a public server and its only job is to relay messages between your phone and your desktop. It does not store any sensitive data.
2.  **The Desktop Client:** A smart Python client that runs on your desktop computer. It connects to the relay server, gets a unique ID, and listens for commands from your phone.
3.  **The Android App:** The mobile app that allows you to control the desktop client. You connect it to your desktop by simply scanning a QR code, and from then on, you can start and monitor jobs from anywhere with an internet connection.

---

## Setup and Installation

### Step 0: Prerequisites

Before you begin, make sure you have the following software installed on your computer:

-   **Python 3:** [Download Python](https://www.python.org/downloads/)
-   **Git:** [Download Git](https://git-scm.com/downloads)

### Step 1: Get the Code

First, clone the repository to your local machine using Git:
```bash
git clone <repository_url>
cd <repository_directory>
```
*Note: Replace `<repository_url>` and `<repository_directory>` with the actual URL and folder name.*

### Step 2: Set up the Relay Server

The relay server must be run on a machine with a public IP address (like a cloud server) so that both the desktop client and the mobile app can connect to it.

**Instructions (for any OS):**
```bash
# Navigate to the relay server directory
cd /app/relay-server

# Install the required Python packages
pip install -r requirements.txt

# Run the server
python server.py
```
The server will now be running and listening for connections on port 5001.

### Step 3: Set up the Desktop Client

The desktop client runs on the computer where you want to execute the Hashcat jobs. The setup is slightly different for Windows and macOS/Linux.

---

#### **For Windows Users:**

Open **Command Prompt (cmd.exe)** and run the following commands:

```cmd
:: Navigate to the desktop client directory
cd app\hashkitty-gui

:: Create a Python virtual environment
python -m venv venv

:: Activate the virtual environment
venv\Scripts\activate

:: Install the required Python packages
pip install -r requirements.txt

:: Run the client
python server.py
```

---

#### **For macOS & Linux Users:**

Open your **Terminal** and run the following commands:

```bash
# Navigate to the desktop client directory
cd /app/hashkitty-gui

# Create a Python virtual environment
python3 -m venv venv

# Activate the virtual environment
source venv/bin/activate

# Install the required Python packages
pip install -r requirements.txt

# Run the client
python server.py
```
---

Upon starting, the desktop client will connect to the relay server and display a **QR code** in the terminal. You will scan this QR code with the Android app to pair the devices.

### Step 4: Set up the Android Application

**Building the App:**
To build the Android application, you need to have Android Studio installed. Open the project in Android Studio and build it.

**Connecting the App:**
1.  Open the app and navigate to the **PC Connect** tab.
2.  Scan the QR code displayed in the terminal by the desktop client.
3.  The app will now be securely connected to your desktop client via the relay server.

## How to Use

1.  Ensure the Relay Server and Desktop Client are running.
2.  Connect the Android app by scanning the QR code.
3.  Navigate to the **Input** tab to specify the hash file and other parameters.
4.  Go to the **Attack** tab and press "Start Remote Attack".
5.  You can monitor the progress and see the results in the **Terminal** and **Output** tabs. The connection will persist even if you close and reopen the app, or if your network changes.