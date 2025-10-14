# File Dictionary

This file provides a detailed breakdown of all non-ignored files in the repository. The goal is to make it clear what each file does, what its key components are, and how it connects to the rest of the application.

---

## Root Directory

-   `AGENTS.md`: Serves as a central index for the project's documentation in the `/docs` directory.
-   `DOCS.md`: Provides a high-level overview of the HashKitty project architecture, its main components (JavaFX app, Android app), and basic setup instructions.
-   `LICENSE`: The full text of the GNU General Public License, version 3, under which this project is licensed.
-   `README.md`: The main entry point for new users. It describes the project's architecture, provides setup instructions for all components, and gives a brief overview of how to use the application.
-   `TODO.md`: A list of planned features, bug fixes, and long-term goals for the project, categorized by priority.
-   `build.gradle.kts`: The root Gradle build script for the entire project, primarily for the Android application.
-   `check_potfile.sh`: A utility shell script for checking the contents of the hashcat potfile.
-   `get_all_modes.sh`: A utility shell script to get a list of all available hashcat modes.
-   `get_modes.sh`: A utility shell script to get a filtered list of hashcat modes.
-   `get_type_from_mode.sh`: A utility shell script to get the hash type from a given hashcat mode number.
-   `gradle.properties`: Configuration properties for the Gradle build system.
-   `gradlew` & `gradlew.bat`: The Gradle wrapper scripts for Unix-based systems and Windows, respectively. They allow building the project without needing to install Gradle manually.
-   `modes.txt`: A text file containing a list of hashcat modes, likely used by the utility scripts.
-   `potfile`: The hashcat potfile, which stores cracked passwords to avoid re-cracking them in future sessions.
-   `settings.gradle.kts`: The root Gradle settings script, which defines the project structure for the build system.
-   `test-hashes-robust.txt` & `test-hashes-short.txt`: Text files containing sample hashes used for testing the application's cracking functionality.

---

## `/docs` Directory

This directory contains all the detailed documentation for the project.

-   `INDEX.md`: The main index file for the documentation.
-   `UI_UX.md`: Outlines general UI and UX requirements and guidelines for the application.
-   `auth.md`: Placeholder for authentication-related documentation.
-   `conduct.md`: The code of conduct for all project contributors.
-   `data_layer.md`: Specifies the requirements for the application's data layer, including connection types (Relay vs. Direct).
-   `fauxpas.md`: A list of common mistakes and anti-patterns to avoid during development.
-   `file_dictionary.md`: This file. A detailed description of all files in the repository.
-   `misc.md`: A document for any miscellaneous information that doesn't fit into other categories.
-   `performance.md`: Guidelines and requirements related to the application's performance.
-   `screens.md`: Detailed requirements, features, and user feedback for each screen in the application.
-   `task_flow.md`: Information about the project's task management and development process.
-   `testing.md`: Guidelines and requirements for writing and running tests.
-   `workflow.md`: A description of the standard development workflow for this project.

---

## `/hashkitty-java` Directory

This directory contains the source code and build files for the JavaFX desktop application.

-   `gradlew` & `gradlew.bat`: Gradle wrapper scripts specific to the Java project.
-   `settings.gradle`: The Gradle settings file for the `hashkitty-java` project, which includes the `app` module.

---

## `/hashkitty-java/app` Directory

-   `build.gradle.kts`: The Gradle build script for the `hashkitty-java` application module. It defines dependencies for libraries like JavaFX, Java-WebSocket, Gson, ZXing (for QR codes), and JSch (for SSH).

---

## `/hashkitty-java/app/src/main/java/hashkitty/java` Directory

This directory contains the Java source code for the desktop application.

-   `App.java`:
    *   **Core Responsibility:** The main entry point for the JavaFX desktop application. It initializes the primary stage, scene, and main layout.
    *   **Key Components:** The `start(Stage primaryStage)` method sets up the entire UI, including the `TabPane` that holds all the different screens. It also creates instances of the core service managers like `HashcatManager` and `RelayProcessManager`.
    *   **Interactions:** This class acts as the central orchestrator. It loads the FXML for each screen, gets the controller instances, and injects dependencies between them (e.g., passing the `HashcatManager` to the `AttackController`).
    *   **Workflow Role:** It handles the application's startup and shutdown lifecycle. On startup, it builds the UI and starts the relay server. On shutdown, it ensures all background services are properly terminated.

-   `attack/AttackController.java`:
    *   **Core Responsibility:** Manages the UI and logic for the "Attack" tab, handling user input for configuring and launching hashcat attacks.
    *   **Key Components:** `@FXML` annotated fields for UI elements from `Attack.fxml`. The `initialize()` method populates UI controls, and `startAttack()` gathers user input to launch the cracking process via the `HashcatManager`.
    *   **Interactions:** Directly interacts with the `HashcatManager` to start and stop attacks. It is initialized by the `App` class.
    *   **Workflow Role:** This is the primary user interface for the core cracking functionality of the application.

-   `hashcat/HashcatManager.java`:
    *   **Core Responsibility:** A wrapper around the `hashcat` command-line executable. It builds the correct command-line arguments, executes the process, and monitors its output.
    *   **Key Components:** `startAttackWithFile()` and `startAttackWithString()` are the main methods to launch a `hashcat` process. A private thread is created to monitor the process's output stream and parse it for cracked passwords.
    *   **Interactions:** Instantiated by the `App` class and used by the `AttackController`. The `App` class provides callbacks to update the UI when a password is cracked.
    *   **Workflow Role:** Abstracts the complexity of managing the `hashcat` command-line tool.

-   `hashtopolis/HashtopolisClient.java`:
    *   **Core Responsibility:** A client for interacting with a Hashtopolis server's REST API.
    *   **Key Components:** Uses the `OkHttp` library to make HTTP requests to the Hashtopolis API and `Gson` to deserialize the JSON response into `HashtopolisTask` objects.
    *   **Interactions:** This would be used by a `HashtopolisController` to fetch and display task information from a Hashtopolis server.

-   `learn/LearnController.java`:
    *   **Core Responsibility:** The controller for the "Learn" screen.
    *   **Key Components:** This is currently a placeholder controller for the static "Learn" screen, which is defined in `Learn.fxml`.

-   `model/HashtopolisTask.java`:
    *   **Core Responsibility:** A data model class that represents a single cracking task from a Hashtopolis server.
    *   **Key Components:** Contains fields for `taskId`, `taskName`, etc., with `@SerializedName` annotations for Gson to map the JSON fields to the class properties.

-   `model/RemoteConnection.java`:
    *   **Core Responsibility:** A data model class that represents a saved remote SSH connection.
    *   **Key Components:** Uses JavaFX `StringProperty` objects to allow for easy data binding with UI components like `ListView`. The core data (`name`, `connectionString`) is kept separate from the properties for robust serialization.

-   `relay/RelayClient.java`:
    *   **Core Responsibility:** A WebSocket client for connecting to the standalone `gokitty-relay` server.
    *   **Key Components:** Extends `WebSocketClient` from the `Java-WebSocket` library. It handles joining a room and sending/receiving messages, using `Gson` to serialize/deserialize the message objects.
    *   **Interactions:** Instantiated by the `App` class to connect to the relay server and receive commands from the mobile client.

-   `relay/RelayProcessManager.java`:
    *   **Core Responsibility:** Manages the lifecycle of the external `gokitty-relay` executable.
    *   **Key Components:** Uses `ProcessBuilder` to start and stop the standalone relay server process.
    *   **Interactions:** Used by the `App` class to automatically start the relay server when the desktop application launches.

-   `server/DirectServer.java`:
    *   **Core Responsibility:** A WebSocket server for a direct, one-to-one connection with a single client, intended for use on a local network.
    *   **Interactions:** This provides an alternative to the relay server for local connections.

-   `server/RelayServer.java`:
    *   **Core Responsibility:** A WebSocket server that acts as a relay, enabling multiple clients to communicate by joining "rooms."
    *   **Interactions:** This is the Java-based relay server that was used before the standalone Go relay was introduced.

-   `settings/SettingsController.java`:
    *   **Core Responsibility:** The controller for the "Settings" screen.
    *   **Key Components:** Manages the `ListView` of remote connections and the theme selector `ComboBox`.
    *   **Interactions:** The `App` class injects the `ObservableList` of remote connections into this controller. It calls methods on the `App` class (`handleImport`, `handleExport`) to trigger file choosers and dialogs.

-   `setup/HashcatSetupController.java`:
    *   **Core Responsibility:** The controller for the "Hashcat Setup" screen.
    *   **Key Components:** This is a placeholder controller for the static setup screen defined in `HashcatSetup.fxml`.

-   `sniffer/SniffController.java`:
    *   **Core Responsibility:** The controller for the "Sniff" screen, which is used for remote packet capture.
    *   **Key Components:** Allows the user to select a remote connection and start/stop a sniffing session. It also includes a feature to analyze local PCAP files using `tshark`.
    *   **Interactions:** It uses the `SniffManager` to handle the actual SSH connection and remote command execution.

-   `sniffer/SniffManager.java`:
    *   **Core Responsibility:** Manages remote packet sniffing sessions over SSH.
    *   **Key Components:** Uses the `JSch` library to connect to a remote device, execute a sniffing command like `tcpdump`, and stream the output back.
    *   **Interactions:** Used by the `SniffController` to perform the remote sniffing operations.

-   `util/ErrorUtil.java`:
    *   **Core Responsibility:** A utility class for displaying standardized error dialogs in a thread-safe manner.
    *   **Key Components:** The `showError` method ensures that the `Alert` dialog is always shown on the JavaFX Application Thread by using `Platform.runLater()`.

-   `util/FileUtil.java`:
    *   **Core Responsibility:** A utility class for file-related operations, such as downloading a file from a URL to a temporary local file.

-   `util/HhkUtil.java`:
    *   **Core Responsibility:** A utility class for handling the import and export of application settings to and from password-protected `.hhk` (zip) files.
    *   **Key Components:** Uses the `zip4j` library to create and read password-protected zip archives and `Gson` to serialize/deserialize the list of `RemoteConnection` objects to/from a JSON file within the archive.

-   `util/HibpUtil.java`:
    *   **Core Responsibility:** A utility class to check passwords against the Have I Been Pwned (HIBP) Pwned Passwords API.
    *   **Key Components:** Hashes the password with SHA-1 and uses the k-Anonymity model to securely check for breaches without sending the full password.

-   `util/NetworkUtil.java`:
    *   **Core Responsibility:** A utility class for network-related operations, primarily to find the local IP address of the machine for the relay server.

-   `util/NormalizationUtil.java`:
    *   **Core Responsibility:** A utility for cleaning and normalizing hash files. It can extract hashes from formats like `user:hash` and write them to a new temporary file.

-   `util/QRCodeUtil.java`:
    *   **Core Responsibility:** A utility class for generating QR code images.
    *   **Key Components:** Uses the `ZXing` library to encode a given string (typically the relay server connection string) into a QR code `Image` that can be displayed in the JavaFX UI.

---
