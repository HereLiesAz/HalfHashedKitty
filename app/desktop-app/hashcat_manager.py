import os
import requests
import subprocess
import logging
import platform

logging.basicConfig(level=logging.INFO)

def get_gokitty_dir():
    """Returns the path to the .gokitty directory in the user's home folder."""
    home_dir = os.path.expanduser("~")
    return os.path.join(home_dir, ".gokitty")

def get_hashcat_dir():
    """Returns the path to the hashcat directory within .gokitty."""
    # The extracted folder from the 7z archive is named 'hashcat-7.1.2'
    return os.path.join(get_gokitty_dir(), "hashcat-7.1.2")

def get_hashcat_executable_path():
    """Returns the full path to the hashcat executable based on the OS."""
    hashcat_dir = get_hashcat_dir()
    if platform.system() == "Windows":
        return os.path.join(hashcat_dir, "hashcat.exe")
    else:
        # For Linux and macOS
        return os.path.join(hashcat_dir, "hashcat.bin")

def download_and_extract_hashcat():
    """
    Downloads and extracts hashcat using the system's 7z command
    if it's not already present.
    """
    gokitty_dir = get_gokitty_dir()
    hashcat_dir = get_hashcat_dir()

    if os.path.exists(get_hashcat_executable_path()):
        logging.info(f"[*] Hashcat executable already found at {get_hashcat_executable_path()}")
        return True

    os.makedirs(gokitty_dir, exist_ok=True)

    url = "https://hashcat.net/files/hashcat-7.1.2.7z"
    archive_path = os.path.join(gokitty_dir, "hashcat.7z")

    try:
        logging.info("[*] Downloading Hashcat v7.1.2...")
        response = requests.get(url, stream=True)
        response.raise_for_status()
        with open(archive_path, "wb") as f:
            for chunk in response.iter_content(chunk_size=8192):
                f.write(chunk)

        logging.info(f"[*] Extracting Hashcat to {gokitty_dir}...")
        # Use the 7z command-line tool for extraction
        # The -y switch assumes "yes" to all queries, preventing hangs.
        subprocess.run(
            ["7z", "x", archive_path, f"-o{gokitty_dir}", "-y"],
            check=True,
            capture_output=True,
            text=True
        )

        logging.info("[*] Hashcat setup complete.")
        return True
    except FileNotFoundError:
        logging.error("Failed to extract hashcat: '7z' command not found. Please install 7-Zip and ensure it is in your system's PATH.")
        return False
    except requests.exceptions.RequestException as e:
        logging.error(f"Failed to download hashcat: {e}")
        return False
    except subprocess.CalledProcessError as e:
        logging.error(f"Failed to extract hashcat using 7z. Return code: {e.returncode}")
        logging.error(f"Stderr: {e.stderr}")
        logging.error(f"Stdout: {e.stdout}")
        return False
    except Exception as e:
        logging.error(f"An unexpected error occurred: {e}")
        return False
    finally:
        if os.path.exists(archive_path):
            os.remove(archive_path)

if __name__ == '__main__':
    if download_and_extract_hashcat():
        print(f"Hashcat setup successful. Executable is at: {get_hashcat_executable_path()}")
    else:
        print("Failed to set up hashcat.")