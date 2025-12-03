# Project TODO List

This document outlines the planned work for the HashKitty project, aligned with the `PRODUCTION_ROADMAP.md`.

## Phase 1: Stabilization and Critical Fixes (High Priority)
- [x] **Fix Localization Loading:** Update `App.java` to load `messages.properties` ResourceBundle to prevent startup crashes.
- [x] **Refactor `HashcatManager`:** Fix command construction logic to ensure correct argument order and eliminate duplicate arguments.
- [ ] **Refactor `AttackController`:** Standardize UI creation (FXML vs Programmatic) and resolve hash identification ambiguity.
- [x] **Verify Build & Tests:** Ensure the project builds cleanly and passes all tests (`./gradlew test`).

## Phase 2: Feature Completion (UI Parity)
- [ ] **Implement Wordlist Tab:** Create controller and UI for selecting wordlists.
- [ ] **Implement Mask Tab:** Create controller and UI for mask creation.
- [ ] **Implement Terminal Tab:** Add embedded terminal/log viewer.
- [ ] **Implement Hashtopolis Tab:** Add API integration.
- [ ] **Enhance Sniff Screen:** Verify `tshark` integration and improve remote sniffing.

## Phase 3: Packaging and Distribution
- [ ] **Resolve `jpackage`:** Configure correct packaging for Windows, Linux, and macOS.
- [ ] **Bundle Dependencies:** Ensure `hashcat` is correctly bundled or downloaded.

## Phase 4: Documentation and Polish
- [ ] **Finalize Documentation:** Complete all Markdown files in `docs/`.
- [ ] **Code Cleanup:** Refactor and format code.
- [ ] **Localization:** Add more languages.
- [ ] **Theming:** Refine Dark/Light themes.

## Future Feature Ideas / "Dream App" Integrations
*   See `PRODUCTION_ROADMAP.md` and previous `TODO.md` for long-term goals like Cloud Cracking, AI analysis, etc.
