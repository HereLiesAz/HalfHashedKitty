# Screens

This document outlines the requirements and feedback for the various screens within the application.

## Attack Screen
- **[DONE]** ~~There's no visual cue that anything happened when the attack button is pressed.~~ (UI now disables during attack).
- **[DONE]** ~~Users should be able to manually type in the IP address of a hashcat or hashkitty server.~~ (This is handled by the remote relay connection feature).
- **[DONE]** Added advanced options: `--force`, `-O` (optimized kernels), and `-w` (workload profile).

## Wordlist Screen
- **[DONE]** ~~There needs to be a file explorer that allows the user to find a file on the PC, on the phone, or enter a web URL.~~ (PC file selection and web URL input are now implemented in the desktop app).

## Mask Screen
- **[DONE]** ~~Currently, neither button on the Mask screen does anything. This needs to be implemented.~~ (Helper buttons for building masks are now implemented).

## Pi Control & PC Control Screens
- The Pi Control and PC Control screens need to be combined into a single, intelligent interface.
- The interface should be smart enough to tell the difference between the two device types, if necessary.

## Learn Screen
- **[DONE]** ~~A "Learn" AzMenuItem must be added.~~
- **[DONE]** ~~The Learn screen should be able to teach a 13-year-old what hashcat is, how it works, and how to use it.~~
- **[DONE]** ~~It should provide the same level of educational content for Hashtopolis.~~

## Settings Screen
- **[DONE]** ~~A "Settings" AzMenuItem must be added.~~
- **[DONE]** ~~The screen must have a list of saved remote connections.~~
- **[DONE]** ~~It must provide an option to back up all saved remotes into a password-protected `.hhk` file (which is a `.zip` file containing `.json` files, with the extension renamed to `.hhk`).~~
- **[DONE]** ~~It must provide an option to save a remote session as a single JSON file.~~
- **[DONE]** ~~It must include an option to load remote session JSON and `.hhk` files.~~

## Hashtopolis Screen
- **[DONE]** ~~Hashtopolis should have its own dedicated screen.~~ (Foundational UI has been implemented).

## Sniff Screen
- A new "Sniff" screen needs to be created, which will replace the rest of the Pi Control screen.
- On the Sniff screen, phones capable of capturing pcap files with root and monitor mode should be able to do so.
- Users should also be able to connect to a Raspberry Pi to perform these actions, allowing the phone to manage the process remotely.

## Connect Screen
- **[DONE]** ~~The "PC Connect" screen/feature should be renamed to simply "Connect".~~ (This is now the "Mobile Connection" box on desktop and "Connect" on Android).
- **[DONE]** ~~The QR code scanner should be able to connect to both PCs and Raspberry Pis.~~ (Desktop app now supports generating QR codes for remote relays, and Android app can connect to them).
- **[DONE]** The Android UI has been refactored to use a `Switch` for the connection type and to correctly size the camera view.