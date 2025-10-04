#!/bin/bash
set -e

# Navigate to the directory where the script is located to ensure correct path resolution
cd "$(dirname "$0")"

echo "--- Starting PyInstaller build in $(pwd) ---"

# Run PyInstaller with output paths relative to the current directory
python3 -m PyInstaller \
    --onefile \
    --windowed \
    --name HashKittyDesktop \
    --distpath ./dist \
    --workpath ./build \
    main_gui.py

echo "--- PyInstaller build finished successfully ---"
echo "The executable can be found in the 'app/desktop-app/dist' directory."