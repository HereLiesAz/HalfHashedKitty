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

## Future Feature Ideas / "Dream App" Integrations

Based on an analysis of Hashcat and the broader security ecosystem, here are suggestions for features that would make this a true "dream app" for security professionals.

### 1. Advanced Wordlist & Rule Management
- **Wordlist Generation Suite:**
  - **`CeWL` Integration:** Crawl target websites to generate custom wordlists.
  - **`CUPP` Integration:** Generate wordlists based on a target's personal information.
  - **`Mentalist` Integration:** Provide a graphical interface for creating complex, rule-based wordlists.
- **Online Wordlist Intelligence:**
  - Connect to online repositories like **WeakPass** or **Hashes.org** to download and manage massive, up-to-date wordlists.

### 2. Intelligent Hash Identification & Analysis
- **Automated Hash ID:** Integrate a tool like `hash-identifier` to automatically detect the hash type.
- **Hash Input Normalization:** Automatically clean and format hash files from different sources.

### 3. Expanded Cracking Methodologies
- **Rainbow Table Integration:** Use rainbow tables (e.g., from Ophcrack) for legacy hashes as a first-pass attack.
- **Cloud Cracking as a Service:** Integrate with cloud providers (AWS, GCP, Azure) to rent powerful GPU instances on-demand.

### 4. Post-Cracking Analysis & Reporting
- **Password Analytics Dashboard:** Display detailed statistics on cracked passwords (complexity, common patterns, reuse).
- **Automated Report Generation:** Create professional PDF/HTML reports summarizing the audit.
- **Credential Exposure Check:** Integrate with **Have I Been Pwned (HIBP)** to check if cracked credentials are in public breaches.

### 5. Seamless Integration with the Security Ecosystem
- **Direct Metasploit/John the Ripper Integration:** Allow seamless import of hashes from Metasploit databases or John the Ripper `.pot` files.
- **PCAP File Analysis:** Enhance the "Sniff" feature by integrating **TShark** to automatically extract handshakes and hashes from `.pcap` files.