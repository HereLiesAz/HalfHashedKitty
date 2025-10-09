# Project TODO List

This document outlines the future work planned for the HashKitty project.

## High Priority
- **Full UI Parity:** The Java desktop application's UI must be expanded to include all features and screens present in the Android application, as specified in `AGENTS.md`.
- **[DONE] Implement Settings Screen:** Create the "Settings" screen in the Java application with functionality for managing remote connections, and importing/exporting configurations as `.hhk` files.
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

## Project Chimera Features

### Architecture & Core Platform
- **Modular, API-First Design:** Develop a high-performance backend server (e.g., in Go or Python) that exposes a comprehensive REST API for all platform operations.
- **Hybrid Agent Model:** Support both local and remote cracking agents, allowing users to seamlessly scale from a single machine to a distributed network.

### User Interface & User Experience (UI/UX)
- **Guided Workflow UI:** Create an intuitive web interface that guides users through setting up an attack, abstracting away complex command-line flags.
- **"Attack Planner" Wizard:** Implement an intelligent wizard that recommends a multi-stage attack plan based on user-defined goals (hash type, time budget, resources).
- **Interactive Session Management:** Build a central dashboard for real-time monitoring and control of active attacks (pause, resume, modify).

### Data Management & Workflow
- **Unified Data Store:** Create a centralized repository for hashlists, wordlists, and rules with automatic ingestion, indexing, and de-duplication.
- **"Global Potfile" & Brain Integration:** Implement a system-wide database of all cracked hashes and tested candidates to prevent redundant work.
- **Rule and Mask Visualizer:** Develop a tool to preview the impact of rules and masks on a wordlist, providing keyspace estimates and time-to-completion calculations.

### Distributed Cracking & Scalability
- **Zero-Configuration Agents:** Simplify the process of adding new agents with self-contained, pre-configured binaries that auto-connect and self-benchmark.
- **Dynamic and Adaptive Chunking:** Implement an advanced scheduling algorithm that dynamically adjusts work unit sizes based on real-time agent performance and health.

### Analytics & Reporting
- **Password Pattern Analysis:** Develop an analytics dashboard to identify systemic weaknesses, such as common base words, prefixes/suffixes, and password complexity.
- **Audit-Ready Reporting:** Enable one-click generation of professional, executive-ready reports summarizing audit findings and providing actionable recommendations.
