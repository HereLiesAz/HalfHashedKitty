# Project TODO List

This document outlines the future work planned for the HashKitty project.

## High Priority
- **Full UI Parity:** The Java desktop application's UI must be expanded to include all features and screens present in the Android application, as specified in `AGENTS.md`.
- **Implement Settings Screen:** Create the "Settings" screen in the Java application with functionality for managing remote connections, and importing/exporting configurations as `.hhk` files.
- **Implement Learn Section:** Develop the "Learn" section to provide educational content on hashcat, hashtopolis, and general password cracking concepts.
- **Advanced Attack Configuration:** Move beyond the hardcoded wordlist and allow users to configure different hashcat attack modes (e.g., mask, dictionary, brute-force) from the UI.

## Medium Priority
- **File System Integration:** Implement a file explorer in the Java application to allow users to select local wordlists, rule files, and hash files.
- **"Sniff" Screen Functionality:** Develop the "Sniff" screen for managing remote packet capture on devices like a Raspberry Pi.
- **Direct Connection Mode:** Add the ability for the mobile app to connect directly to the desktop application without relying on the relay server.
- **Robust Error Handling:** Improve error handling and provide more informative feedback to the user for both the client and server applications.

## Low Priority
- **Application Packaging:** Create native installers/packages for the Java application for Windows, macOS, and Linux using `jpackage`.
- **Code Refactoring and Cleanup:** Refactor the codebase to improve maintainability, and add more comprehensive inline documentation.
- **Localization:** Add support for multiple languages in the UI.
- **Theming:** Allow users to choose between light and dark themes for the application.