# Project TODO List

This document outlines potential future work for the HashKitty project. The core desktop application is now feature-complete based on the initial requirements.

## High Priority
- **Full Android Integration:** The final step of the universal relay plan is to compile the `gokitty-relay` Go code into a library and integrate it as a background service into the Android application. This was blocked by environment issues but remains a key goal.
- **Enhanced File System Integration:** Expand the "Attack" tab UI to allow users to select hash files and hashcat rule files directly from the file system, in addition to the existing wordlist selector.

## Medium Priority
- **Expanded Application Packaging:** The project currently includes a `.deb` installer for Linux. This could be expanded by creating native installers for Windows (`.msi`) and macOS (`.dmg`) using `jpackage`.
- **Localization:** Add support for multiple languages to the JavaFX application using resource bundles. This would make the application more accessible to a global audience.

## Low Priority
- **UI/UX Polish:** Continue to refine the user interface and user experience based on feedback.
- **Codebase Health:** Continue to refactor and improve the codebase as new features are added or requirements change.