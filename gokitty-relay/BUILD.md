# Building the Universal Go Relay Server

This document provides instructions on how to cross-compile the standalone `gokitty-relay` server for various target platforms.

## Prerequisites

-   **Go:** Version 1.18 or higher installed on your development machine.

## Build Commands

All commands should be run from the root of the `gokitty-relay` directory (`/app/gokitty-relay`). The output binary will be placed in a `build` directory.

### 1. Linux (Standard x86_64 for Web Servers/Desktops)

This is the standard build for most Linux distributions.

```bash
GOOS=linux GOARCH=amd64 go build -o build/gokitty-relay-linux-amd64 ./cmd/gokitty-relay
```

### 2. Windows (x86_64)

This creates a `.exe` file for modern Windows systems.

```bash
GOOS=windows GOARCH=amd64 go build -o build/gokitty-relay-windows-amd64.exe ./cmd/gokitty-relay
```

### 3. macOS (Apple Silicon - ARM64)

For newer Mac computers with M1/M2/M3 chips.

```bash
GOOS=darwin GOARCH=arm64 go build -o build/gokitty-relay-macos-arm64 ./cmd/gokitty-relay
```

### 4. macOS (Intel - x86_64)

For older, Intel-based Mac computers.

```bash
GOOS=darwin GOARCH=amd64 go build -o build/gokitty-relay-macos-amd64 ./cmd/gokitty-relay
```

### 5. Raspberry Pi (Linux ARM64)

For modern Raspberry Pi models (3, 4, 5) running a 64-bit OS.

```bash
GOOS=linux GOARCH=arm64 go build -o build/gokitty-relay-pi-arm64 ./cmd/gokitty-relay
```

### 6. Raspberry Pi (Linux ARMv6 - Legacy)

For older Raspberry Pi models (1, Zero) running a 32-bit OS.

```bash
GOOS=linux GOARCH=arm GOARM=6 go build -o build/gokitty-relay-pi-armv6 ./cmd/gokitty-relay
```

## Running as a Service

Once you have the compiled binary for your target desktop OS (Linux, Windows, macOS), you can manage it as a background service using the following commands:

```bash
# Install the service
./gokitty-relay-[os-arch] install

# Start the service
./gokitty-relay-[os-arch] start

# Stop the service
./gokitty-relay-[os-arch] stop

# Uninstall the service
./gokitty-relay-[os-arch] uninstall
```