#ifndef HASHCAT_MANAGER_H
#define HASHCAT_MANAGER_H

#include <string>
#include <vector>
#include <functional>
#include <thread>
#include <atomic>

class HashcatManager {
public:
    HashcatManager();
    ~HashcatManager();

    // Ensures hashcat is downloaded and ready to use.
    // Returns true on success, false on failure.
    bool setup_hashcat();

    // Gets the full path to the hashcat executable.
    std::string get_hashcat_executable_path() const;

    // Starts a hashcat attack with the given parameters.
    // The callback is used to stream output lines back to the caller.
    void start_attack(
        const std::vector<std::string>& args,
        std::function<void(const std::string&)> output_callback
    );

    // Checks if an attack is currently running.
    bool is_attacking() const;

private:
    void attack_thread_func(
        std::vector<std::string> args,
        std::function<void(const std::string&)> output_callback
    );

    std::string _hashcat_executable_path;
    std::thread _attack_thread;
    std::atomic<bool> _is_attacking;
};

#endif // HASHCAT_MANAGER_H