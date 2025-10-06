# Screens

This document outlines the requirements and feedback for the various screens within the application.

## Attack Screen
- There's no visual cue that anything happened when the attack button is pressed.
- Users should be able to manually type in the IP address of a hashcat or hashkitty server.

## Wordlist Screen
- There needs to be a file explorer that allows the user to find a file on the PC, on the phone, or enter a web URL.

## Mask Screen
- Currently, neither button on the Mask screen does anything. This needs to be implemented.

## Pi Control & PC Control Screens
- The Pi Control and PC Control screens need to be combined into a single, intelligent interface.
- The interface should be smart enough to tell the difference between the two device types, if necessary.

## Learn Screen
- A "Learn" AzMenuItem must be added.
- The Learn screen should be able to teach a 13-year-old what hashcat is, how it works, and how to use it.
- It should provide the same level of educational content for Hashtopolis.

## Settings Screen
- A "Settings" AzMenuItem must be added.
- The screen must have a list of saved remote connections.
- It must provide an option to back up all saved remotes into a password-protected `.hhk` file (which is a `.zip` file containing `.json` files, with the extension renamed to `.hhk`).
- It must provide an option to save a remote session as a single JSON file.
- It must include an option to load remote session JSON and `.hhk` files.

## Hashtopolis Screen
- Hashtopolis should have its own dedicated screen.

## Sniff Screen
- A new "Sniff" screen needs to be created, which will replace the rest of the Pi Control screen.
- On the Sniff screen, phones capable of capturing pcap files with root and monitor mode should be able to do so.
- Users should also be able to connect to a Raspberry Pi to perform these actions, allowing the phone to manage the process remotely.

## Connect Screen
- The "PC Connect" screen/feature should be renamed to simply "Connect".
- The QR code scanner should be able to connect to both PCs and Raspberry Pis.