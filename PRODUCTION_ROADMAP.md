# Production Roadmap for Half-Hashed Kitty

This document serves as a comprehensive guide for Agent AIs to take the Half-Hashed Kitty project from its current state to a production-ready application.

## Current State Analysis

*   **Core Architecture:** The project is a JavaFX desktop application (`hashkitty-java`) acting as a frontend for `hashcat`. It communicates with a Go-based relay server (`gokitty-relay`) and an Android client.
*   **UI/UX:** The desktop app has a tabbed interface defined in FXML. Some screens ("Attack", "Sniff", "Settings") are partially implemented, while others ("Wordlist", "Mask", "Terminal") are placeholders.
*   **Critical Bugs:**
    *   **Localization Failure:** `App.java` fails to load the `messages.properties` ResourceBundle when loading FXML files, which will cause the application to crash on startup due to unresolvable resource keys (e.g., `%attack.title`).
    *   **Command Construction Logic:** `HashcatManager.java` has flawed logic for building the hashcat command, leading to incorrect argument order (putting the target before the hash file) and duplicate arguments in `startAttackWithString`.
    *   **Inconsistent UI Construction:** `AttackController.java` mixes FXML with programmatic UI element creation, leading to maintenance issues.
*   **Missing Features:**
    *   Full UI parity with the Android app is not yet achieved.
    *   Controllers for several tabs are missing.
    *   Packaging/Installation is not finalized.

## Step-by-Step Guide to Production

### Phase 1: Stabilization and Critical Fixes

1.  **Fix Localization Loading in `App.java`**
    *   **Action:** Modify `loadFxml` and screen loading methods in `App.java` to load `messages.properties` as a `ResourceBundle` and pass it to `FXMLLoader`.
    *   **Goal:** Prevent application crash on startup.

2.  **Refactor `HashcatManager.java`**
    *   **Action:** Rewrite `buildCommand`, `startAttackWithFile`, and `startAttackWithString` to strictly follow the `hashcat [options] hashfile target` command structure. Remove the complex and buggy argument swapping logic in `startAttackInternal`.
    *   **Goal:** Ensure `hashcat` is invoked correctly.

3.  **Refactor `AttackController.java`**
    *   **Action:** Move programmatic UI creation (Dictionary/Mask inputs) to FXML (using `visible` / `managed` properties to toggle) OR ensure the programmatic approach is robust and consistent. Address the `identifyHashType` ambiguity.
    *   **Goal:** Improve code maintainability and UI stability.

4.  **Verify Build and Test Suite**
    *   **Action:** Ensure `./gradlew clean build` and `./gradlew test` pass. Add unit tests for `HashcatManager` command generation to prevent regressions.

### Phase 2: Feature Completion (UI Parity)

5.  **Implement Missing Screens**
    *   **Action:** specific implementations for:
        *   **Wordlist Tab:** File browser for selecting/managing wordlists.
        *   **Mask Tab:** Interface for creating and saving masks.
        *   **Terminal Tab:** Embedded terminal or log viewer for raw hashcat output.
        *   **Hashtopolis Tab:** API integration for Hashtopolis.
    *   **Goal:** Fulfill the "Full UI Parity" requirement.

6.  **Enhance "Sniff" Feature**
    *   **Action:** Verify `tshark` integration. Add proper checks for `tshark` installation and guide the user if missing. Implement the remote packet capture logic using `JSch` (SSH) fully.

### Phase 3: Packaging and Distribution

7.  **Resolve `jpackage` Issues**
    *   **Action:** Revisit `jpackage` configuration. Ensure all dependencies (including JavaFX modules) are correctly handled. Consider using a tool like `JReleaser` or `Badass JLink Plugin` if standard `jpackage` proves difficult.
    *   **Goal:** Produce `.exe`, `.dmg`, and `.deb` installers.

8.  **Bundle Dependencies**
    *   **Action:** Ensure `hashcat` binaries for supported platforms are either bundled or downloaded reliably during setup (the current setup does this, but it needs verification).

### Phase 4: Documentation and Polish

9.  **Finalize Documentation**
    *   **Action:** Complete `docs/screens.md`, `docs/workflow.md`, etc. Create a user manual.
    *   **Goal:** Ensure the project is maintainable and usable.

10. **Code Cleanup**
    *   **Action:** Remove unused code, standardize comments, and apply consistent formatting.

## Immediate Actions for Agent

The current agent should proceed with **Phase 1** immediately.
