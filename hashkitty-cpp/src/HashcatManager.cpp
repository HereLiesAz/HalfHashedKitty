#include "HashcatManager.h"
#include <iostream>
#include <fstream>
#include <cstdlib> // For system()
#include <stdexcept>
#include <cstdio> // For popen, pclose, fgets
#include <memory> // For std::unique_ptr
#include <filesystem> // For std::filesystem

#if defined(_WIN32)
#include <windows.h>
#include <shlobj.h>
#else
#include <sys/types.h>
#include <pwd.h>
#include <unistd.h>
#endif

// Helper function to get the user's home directory
std::string get_home_directory() {
#if defined(_WIN32)
    CHAR path[MAX_PATH];
    if (SUCCEEDED(SHGetFolderPathA(NULL, CSIDL_PROFILE, NULL, 0, path))) {
        return std::string(path);
    }
    return "";
#else
    const char* homedir;
    if ((homedir = getenv("HOME")) == NULL) {
        homedir = getpwuid(getuid())->pw_dir;
    }
    return std::string(homedir);
#endif
}

std::string get_gokitty_dir() {
    return get_home_directory() + "/.gokitty";
}

HashcatManager::HashcatManager() : _is_attacking(false) {
    _hashcat_executable_path = get_hashcat_executable_path();
}

HashcatManager::~HashcatManager() {
    if (_attack_thread.joinable()) {
        _attack_thread.join();
    }
}

std::string HashcatManager::get_hashcat_executable_path() const {
    std::string hashcat_dir = get_gokitty_dir() + "/hashcat-7.1.2";
#if defined(_WIN32)
    return hashcat_dir + "/hashcat.exe";
#else
    return hashcat_dir + "/hashcat.bin";
#endif
}

bool file_exists(const std::string& path) {
    std::ifstream f(path.c_str());
    return f.good();
}

bool HashcatManager::setup_hashcat() {
    if (file_exists(_hashcat_executable_path)) {
        std::cout << "[*] Hashcat executable already found." << std::endl;
        return true;
    }

    std::string gokitty_dir = get_gokitty_dir();

    // Use C++17 filesystem library for cross-platform directory creation
    try {
        std::filesystem::create_directories(gokitty_dir);
    } catch (const std::filesystem::filesystem_error& e) {
        std::cerr << "Failed to create directory " << gokitty_dir << ": " << e.what() << std::endl;
        return false;
    }

    std::string url = "https://hashcat.net/files/hashcat-7.1.2.7z";
    std::string archive_path = gokitty_dir + "/hashcat.7z";

    // Download using curl
    std::cout << "[*] Downloading Hashcat..." << std::endl;
    std::string download_cmd = "curl -L " + url + " -o " + archive_path;
    int download_result = system(download_cmd.c_str());
    if (download_result != 0) {
        std::cerr << "Failed to download hashcat." << std::endl;
        return false;
    }

    // Extract using 7z
    std::cout << "[*] Extracting Hashcat..." << std::endl;
    std::string extract_cmd = "7z x " + archive_path + " -o" + gokitty_dir + " -y";
    int extract_result = system(extract_cmd.c_str());
    if (extract_result != 0) {
        std::cerr << "Failed to extract hashcat. Make sure '7z' is installed and in your PATH." << std::endl;
        return false;
    }

    // Clean up
    remove(archive_path.c_str());

    if (!file_exists(_hashcat_executable_path)) {
        std::cerr << "Hashcat executable not found after setup." << std::endl;
        return false;
    }

    std::cout << "[*] Hashcat setup complete." << std::endl;
    return true;
}

void HashcatManager::start_attack(
    const std::vector<std::string>& args,
    std::function<void(const std::string&)> output_callback) {
    if (_is_attacking) {
        throw std::runtime_error("An attack is already in progress.");
    }
    _is_attacking = true;
    _attack_thread = std::thread(&HashcatManager::attack_thread_func, this, args, output_callback);
    _attack_thread.detach(); // Allow the thread to run independently
}

bool HashcatManager::is_attacking() const {
    return _is_attacking;
}

void HashcatManager::attack_thread_func(
    std::vector<std::string> args,
    std::function<void(const std::string&)> output_callback) {

    std::string command = _hashcat_executable_path;
    for (const auto& arg : args) {
        command += " " + arg;
    }
    // Redirect stderr to stdout
    command += " 2>&1";

    output_callback("Executing command: " + command + "\n\n");

    std::unique_ptr<FILE, decltype(&pclose)> pipe(popen(command.c_str(), "r"), pclose);
    if (!pipe) {
        output_callback("popen() failed!");
        _is_attacking = false;
        return;
    }

    char buffer[128];
    while (fgets(buffer, sizeof(buffer), pipe.get()) != nullptr) {
        output_callback(buffer);
    }

    output_callback("\n--- Attack Finished ---\n");
    _is_attacking = false;
}