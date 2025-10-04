# HashKitty C++ Desktop

This is the C++ desktop application for HashKitty, which provides a GUI for local `hashcat` attacks, a WebSocket server for remote control via the mobile app, and network packet capture (pcap) functionality.

## Dependencies

This project relies on several external libraries. You must install them before building the application.

### Runtime Dependencies

These are required for the application to run correctly, specifically for the automatic setup of `hashcat`.

*   `curl`: Used to download the `hashcat` archive.
*   `7z`: Used to extract the `hashcat` archive.

On Debian/Ubuntu, you can install these with:
```bash
sudo apt-get update
sudo apt-get install -y curl p7zip-full
```

### Build-Time Dependencies

These are required to compile the application from source.

*   A C++17 compliant compiler (e.g., `g++`)
*   `CMake` (version 3.10 or higher)
*   `Boost` (specifically `system` and `thread` components)
*   `libpcap`
*   `libglfw3`
*   `libglew`
*   `libqrencode`

On Debian/Ubuntu, you can install all the required build dependencies with the following command:
```bash
sudo apt-get update
sudo apt-get install -y build-essential cmake libboost-all-dev libpcap-dev libglfw3-dev libglew-dev libqrencode-dev
```

## Building the Application

Once all dependencies are installed, you can build the project using CMake.

1.  **Clone the repository and initialize submodules:**
    ```bash
    git clone <repository_url>
    cd <repository_name>
    git submodule update --init --recursive
    ```

2.  **Create a build directory:**
    ```bash
    cd hashkitty-cpp
    mkdir build
    cd build
    ```

3.  **Configure and build the project:**
    ```bash
    cmake ..
    make
    ```

4.  **Run the application:**
    The executable will be located in the `build` directory.
    ```bash
    ./hashkitty_desktop
    ```