#include "GuiManager.h"
#include "PcapManager.h"
#include "HashcatManager.h"
#include "Server.h"
#include <iostream>
#include <random>
#include <sstream>
#include "nlohmann/json.hpp"

// Use nlohmann::json for convenience
using json = nlohmann::json;

// Helper to generate a simple random room ID
std::string generate_simple_room_id() {
    std::stringstream ss;
    std::random_device rd;
    std::mt19937 gen(rd());
    std::uniform_int_distribution<> distrib(0, 15);
    for (int i = 0; i < 8; ++i) {
        ss << std::hex << distrib(gen);
    }
    return ss.str();
}

int main(int, char**)
{
    // 1. Initialize backend components
    PcapManager pcap_manager;
    HashcatManager hashcat_manager;
    WebSocketServer server;

    // Ensure hashcat is ready before starting
    if (!hashcat_manager.setup_hashcat()) {
        std::cerr << "Failed to set up hashcat. Please check logs. Exiting." << std::endl;
        return 1;
    }

    // 2. Generate a room ID for this session
    std::string room_id = generate_simple_room_id();

    // 3. Initialize the GUI Manager
    GuiManager gui(pcap_manager, hashcat_manager, server, room_id);

    // 4. Set up the server's attack callback
    server.set_attack_callback([&](const std::string& payload_str, connection_hdl hdl) {
        gui.log_to_terminal("--- Received remote attack request ---\n");

        // Parse the attack payload
        json payload = json::parse(payload_str);
        std::vector<std::string> args;
        args.push_back("-m");
        args.push_back(payload.value("mode", "0"));
        args.push_back("-a");
        args.push_back(payload.value("attackMode", "0"));
        args.push_back(payload.value("file", ""));
        if (payload.contains("wordlist") && !payload["wordlist"].empty()) {
            args.push_back(payload["wordlist"]);
        }
        if (payload.contains("rules") && !payload["rules"].empty()) {
            args.push_back("-r");
            args.push_back(payload["rules"]);
        }

        // Define the output callback for this attack
        auto output_callback = [&](const std::string& output) {
            // Log to local GUI terminal
            gui.log_to_terminal(output);

            // Send status update back to the remote client
            json res = {
                {"jobId", payload.value("jobId", "")},
                {"status", "running"},
                {"output", output}
            };
            json wrapper = {
                {"type", "status_update"},
                {"room_id", room_id},
                {"payload", res.dump()}
            };
            server.send_message(hdl, wrapper.dump());
        };

        hashcat_manager.start_attack(args, output_callback);
    });

    // 5. Run the server
    server.run(5001);

    // 6. Initialize and run the GUI
    if (!gui.init("HashKitty C++ Desktop", 1280, 720)) {
        std::cerr << "Failed to initialize GUI. Exiting." << std::endl;
        server.stop();
        return 1;
    }

    gui.run(); // This blocks until the window is closed

    // 7. Clean up
    server.stop();
    gui.shutdown();

    return 0;
}