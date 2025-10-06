# Half-Hashed Kitty: Comprehensive TODO List

This document outlines the necessary improvements, refactoring, and features to be implemented in the Half-Hashed Kitty project. The goal is to modernize the codebase, enhance user experience, and ensure long-term maintainability.

## I. Core Architecture & Refactoring

### 1. **Go Desktop Server (`gokitty`)**
-   [ ] **Modularize the Code:**
    -   [ ] Separate the WebSocket relay logic from the desktop client logic into different packages (e.g., `pkg/relay`, `pkg/client`).
    -   [ ] Move data structures (`Message`, `AttackParams`) into a shared package (e.g., `pkg/models`) to be used by both the client and relay.
    -   [ ] Extract the Hashcat setup and execution logic into its own module (e.g., `pkg/hashcat`).
-   [ ] **Improve Configuration:**
    -   [ ] Replace command-line flags with a configuration file (e.g., `config.yaml`) for better management of settings like relay URL, ports, and Hashcat paths.
    -   [ ] Implement environment variable support for configuration, which is useful for containerized deployments.
-   [ ] **Error Handling & Logging:**
    -   [ ] Implement structured logging (e.g., using `logrus` or `zap`) to provide more context in logs.
    -   [ ] Provide more specific error messages to the Android client to help with debugging.

### 2. **Android Application**
-   [ ] **ViewModel & State Management:**
    -   [ ] Refactor `MainViewModel` to better handle the state of the connection, attack progress, and results.
    -   [ ] Use `StateFlow` or `SharedFlow` to manage UI state in a more reactive way.
-   [ ] **UI/UX Enhancements:**
    -   [ ] Create a dedicated screen for displaying the terminal output from Hashcat, with proper scrolling and text wrapping.
    -   [ ] Add a visual indicator to show when the app is connected to the desktop server.
    -   [ ] Implement a settings screen for configuring the relay server URL.
-   [ ] **Networking:**
    -   [ ] Replace the current WebSocket implementation with a more robust library that handles automatic reconnection (e.g., OkHttp's WebSocket support).
    -   [ ] Improve error handling for network-related issues (e.g., connection lost, server not found).

### 3. **Code Cleanup & Legacy Code Removal**
-   [ ] **Remove Old Projects:**
    -   [ ] Delete the `hashkitty-cpp` directory and all related files.
    -   [ ] Remove the `hashkitty-gui` (Python) and other related scripts.
    -   [ ] Eliminate any other unused scripts and files from the root directory.
-   [ ] **Update `README.md`:**
    -   [ ] Remove all references to the old C++ and Python projects.
    -   [ ] Simplify the setup instructions to focus only on the Go server and the Android app.

## II. Features & Enhancements

### 1. **Attack Management**
-   [ ] **Pause and Resume Attacks:**
    -   [ ] Implement functionality in the Go server to pause and resume Hashcat jobs.
    -   [ ] Add corresponding controls in the Android app.
-   [ ] **Job History:**
    -   [ ] Store a history of past attacks (including parameters and results) on the Android device.
    -   [ ] Allow users to view and re-run previous attacks.

### 2. **Security**
-   [ ] **Secure Communication:**
    -   [ ] Implement WebSocket over TLS (WSS) for the relay server to encrypt communication.
    -   [ ] Add an option in the Android app and Go client to use WSS.
-   [ ] **Input Validation:**
    -   [ ] Enhance the input validation on the Go server to prevent command injection and other potential security vulnerabilities.

## III. Build & Testing

### 1. **Continuous Integration (CI)**
-   [ ] **GitHub Actions:**
    -   [ ] Create a CI pipeline to automatically build and test the Go server on each push.
    -   [ ] Add a separate workflow for building the Android app.
-   [ ] **Automated Testing:**
    -   [ ] Write unit tests for the Go server's business logic (e.g., attack command generation, message handling).
    -   [ ] Implement UI tests for the Android app using Espresso or a similar framework.

### 2. **Build Process**
-   [ ] **Go Server:**
    -   [ ] Create a `Makefile` to simplify the build process for different platforms (Windows, macOS, Linux).
-   [ ] **Android App:**
    -   [ ] Ensure the Gradle build is configured for both debug and release builds.
    -   [ ] Add ProGuard rules to obfuscate and shrink the release version of the app.

## IV. Documentation

-   [ ] **API Documentation:**
    -   [ ] Document the WebSocket API, including all message types and their payloads.
-   [ ] **Code Comments:**
    -   [ ] Add KDocs to all public classes and methods in the Android app.
    -   [ ] Add comments to the Go server's code to explain complex logic.
-   [ ] **User Guide:**
    -   [ ] Create a more detailed user guide with screenshots and examples.