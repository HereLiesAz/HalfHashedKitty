#ifndef GUI_MANAGER_H
#define GUI_MANAGER_H

#include "imgui.h"
#include "imgui_impl_glfw.h"
#include "imgui_impl_opengl3.h"
#include <GL/glew.h>
#include <GLFW/glfw3.h>
#include <string>
#include <vector>
#include <stdexcept>
#include <functional>
#include <mutex>
#include "PcapManager.h" // Include the full definition now

// Forward declarations
class HashcatManager;
class WebSocketServer;

class GuiManager {
public:
    GuiManager(PcapManager& pcap_manager, HashcatManager& hashcat_manager, WebSocketServer& server, const std::string& room_id);
    ~GuiManager();

    bool init(const std::string& title, int width, int height);
    void run();
    void shutdown();

    // Public method to append to the terminal from other threads
    void log_to_terminal(const std::string& message);

private:
    // UI rendering functions
    void render_ui();
    void render_pcap_tab();
    void render_attack_tab();
    void render_server_tab();
    void render_terminal_tab();

    // Helper for QR Code
    void generate_qr_texture();

    GLFWwindow* _window;
    PcapManager& _pcap_manager;
    HashcatManager& _hashcat_manager;
    WebSocketServer& _server;

    // --- GUI State ---

    // PCAP Tab
    std::vector<PcapDevice> _pcap_devices;
    int _selected_pcap_device = 0;
    char _pcap_output_filename[128] = "capture.pcap";

    // Attack Tab
    char _hash_file_buf[128] = "";
    char _hash_mode_buf[32] = "";
    char _attack_mode_buf[32] = "";
    char _wordlist_buf[128] = "";
    char _rules_buf[128] = "";

    // Server Tab
    const std::string& _room_id;
    GLuint _qr_texture = 0;

    // Terminal
    ImGuiTextBuffer _terminal_buffer;
    std::mutex _terminal_mutex;
};

#endif // GUI_MANAGER_H