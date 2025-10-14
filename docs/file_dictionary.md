# File Dictionary

This file contains a list of all non-ignored files in the repository and a brief description of what each file is supposed to do.

---

## Root Directory

-   `AGENTS.md`: Serves as a central index for the project's documentation.
-   `DOCS.md`: Provides an overview of the HashKitty project architecture, components, and setup instructions.
-   `LICENSE`: The GNU General Public License, version 3.
-   `README.md`: A cross-platform tool for simplifying WiFi security auditing and password cracking.
-   `TODO.md`: Outlines the future work planned for the HashKitty project.
-   `build.gradle.kts`: Gradle build script for the root project.
-   `check_potfile.sh`: A shell script to check the potfile.
-   `get_all_modes.sh`: A shell script to get all hashcat modes.
-   `get_modes.sh`: A shell script to get hashcat modes.
-   `get_type_from_mode.sh`: A shell script to get the hash type from a given mode.
-   `gradle.properties`: Gradle properties file.
-   `gradlew`: Gradle wrapper script for Unix-based systems.
-   `gradlew.bat`: Gradle wrapper script for Windows.
-   `modes.txt`: A text file containing a list of hashcat modes.
-   `potfile`: The hashcat potfile, which stores cracked passwords to avoid re-cracking.
-   `settings.gradle.kts`: Gradle settings script for the root project.
-   `test-hashes-robust.txt`: A file containing a list of test hashes for robust testing.
-   `test-hashes-short.txt`: A file containing a short list of test hashes for quick testing.

---

## `/docs` Directory

-   `INDEX.md`: Main index for the project documentation.
-   `UI_UX.md`: General UI and UX requirements.
-   `auth.md`: Authentication-related information.
-   `conduct.md`: Code of conduct for contributors.
-   `data_layer.md`: Requirements for the application's data layer and connectivity.
-   `fauxpas.md`: Common mistakes and how to avoid them.
-   `file_dictionary.md`: This file.
-   `misc.md`: Miscellaneous documentation.
-   `performance.md`: Performance-related guidelines and requirements.
-   `screens.md`: Details about the application's screens and their functionality.
-   `task_flow.md`: Information about task flow and management.
-   `testing.md`: Guidelines and requirements for testing.
-   `workflow.md`: Information about the development workflow.

---

## `/hashkitty-java` Directory

-   `gradlew`: Gradle wrapper script for Unix-based systems.
-   `gradlew.bat`: Gradle wrapper script for Windows.
-   `settings.gradle`: Gradle settings file for the `hashkitty-java` project.

---

## `/hashkitty-java/app` Directory

-   `build.gradle.kts`: Gradle build script for the `hashkitty-java` application module.

---

## `/hashkitty-java/app/src/main/java/hashkitty/java` Directory

-   `App.java`: The main class for the HashKitty JavaFX desktop application.
-   `attack/AttackController.java`: The controller for the attack screen.
-   `hashcat/HashcatManager.java`: Manages the execution of the `hashcat` command-line tool.
-   `hashtopolis/HashtopolisClient.java`: A client for interacting with a Hashtopolis server API.
-   `learn/LearnController.java`: The controller for the learn screen.
-   `model/HashtopolisTask.java`: Represents a single cracking task from a Hashtopolis server.
-   `model/RemoteConnection.java`: Represents a saved remote connection, typically for an SSH target.
-   `relay/RelayClient.java`: A WebSocket client for connecting to the standalone gokitty-relay server.
-   `relay/RelayProcessManager.java`: Manages the lifecycle of the external gokitty-relay executable.
-   `server/DirectServer.java`: A WebSocket server that handles a direct, one-to-one connection with a single client.
-   `server/RelayServer.java`: A WebSocket server that acts as a relay, enabling multiple clients to communicate by joining "rooms".
-   `settings/SettingsController.java`: The controller for the settings screen.
-   `setup/HashcatSetupController.java`: The controller for the Hashcat setup screen.
-   `sniffer/SniffController.java`: The controller for the sniffer screen.
-   `sniffer/SniffManager.java`: Manages remote packet sniffing sessions over SSH.
-   `util/ErrorUtil.java`: A utility class for displaying standardized error dialogs.
-   `util/FileUtil.java`: A utility class for file-related operations.
-   `util/HhkUtil.java`: A utility class for handling the import and export of application settings.
-   `util/HibpUtil.java`: A utility class to check passwords against the Have I Been Pwned (HIBP) Pwned Passwords API.
-   `util/NetworkUtil.java`: A utility class for network-related operations.
-   `util/NormalizationUtil.java`: A utility for cleaning and normalizing hash files.
-   `util/QRCodeUtil.java`: A utility class for generating QR code images using the ZXing library.

---
