#include "GuiManager.h"
#include "HashcatManager.h"
#include "Server.h"
#include <iostream>
#include <qrencode.h>
#include <vector>

// Helper to handle GLFW errors
static void glfw_error_callback(int error, const char* description) {
    std::cerr << "GLFW Error " << error << ": " << description << std::endl;
}

GuiManager::GuiManager(PcapManager& pcap, HashcatManager& hashcat, WebSocketServer& server, const std::string& room_id)
    : _window(nullptr), _pcap_manager(pcap), _hashcat_manager(hashcat), _server(server), _room_id(room_id) {}

GuiManager::~GuiManager() {
    shutdown();
}

void GuiManager::log_to_terminal(const std::string& message) {
    std::lock_guard<std::mutex> lock(_terminal_mutex);
    _terminal_buffer.appendf("%s", message.c_str());
}

bool GuiManager::init(const std::string& title, int width, int height) {
    glfwSetErrorCallback(glfw_error_callback);
    if (!glfwInit()) return false;

    glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
    glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
    glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

    _window = glfwCreateWindow(width, height, title.c_str(), nullptr, nullptr);
    if (!_window) { glfwTerminate(); return false; }
    glfwMakeContextCurrent(_window);
    glfwSwapInterval(1);

    if (glewInit() != GLEW_OK) {
        std::cerr << "Failed to initialize GLEW" << std::endl;
        return false;
    }

    IMGUI_CHECKVERSION();
    ImGui::CreateContext();
    ImGuiIO& io = ImGui::GetIO();
    io.ConfigFlags |= ImGuiConfigFlags_NavEnableKeyboard;
    io.ConfigFlags |= ImGuiConfigFlags_DockingEnable;
    io.ConfigFlags |= ImGuiConfigFlags_ViewportsEnable;

    ImGui::StyleColorsDark();
    ImGuiStyle& style = ImGui::GetStyle();
    if (io.ConfigFlags & ImGuiConfigFlags_ViewportsEnable) {
        style.WindowRounding = 0.0f;
        style.Colors[ImGuiCol_WindowBg].w = 1.0f;
    }

    ImGui_ImplGlfw_InitForOpenGL(_window, true);
    ImGui_ImplOpenGL3_Init("#version 330");

    // Load pcap devices
    try {
        _pcap_devices = _pcap_manager.get_all_devs();
    } catch (const std::exception& e) {
        log_to_terminal("Error getting pcap devices: " + std::string(e.what()) + "\n");
    }

    generate_qr_texture();

    return true;
}

void GuiManager::run() {
    while (!glfwWindowShouldClose(_window)) {
        glfwPollEvents();
        ImGui_ImplOpenGL3_NewFrame();
        ImGui_ImplGlfw_NewFrame();
        ImGui::NewFrame();
        ImGui::DockSpaceOverViewport();
        render_ui();
        ImGui::Render();
        int display_w, display_h;
        glfwGetFramebufferSize(_window, &display_w, &display_h);
        glViewport(0, 0, display_w, display_h);
        glClearColor(0.45f, 0.55f, 0.60f, 1.00f);
        glClear(GL_COLOR_BUFFER_BIT);
        ImGui_ImplOpenGL3_RenderDrawData(ImGui::GetDrawData());
        ImGuiIO& io = ImGui::GetIO();
        if (io.ConfigFlags & ImGuiConfigFlags_ViewportsEnable) {
            GLFWwindow* backup_current_context = glfwGetCurrentContext();
            ImGui::UpdatePlatformWindows();
            ImGui::RenderPlatformWindowsDefault();
            glfwMakeContextCurrent(backup_current_context);
        }
        glfwSwapBuffers(_window);
    }
}

void GuiManager::shutdown() {
    if (_qr_texture) {
        glDeleteTextures(1, &_qr_texture);
        _qr_texture = 0;
    }
    ImGui_ImplOpenGL3_Shutdown();
    ImGui_ImplGlfw_Shutdown();
    ImGui::DestroyContext();
    if (_window) glfwDestroyWindow(_window);
    glfwTerminate();
}

void GuiManager::render_ui() {
    if (ImGui::BeginMainMenuBar()) {
        if (ImGui::BeginMenu("File")) {
            if (ImGui::MenuItem("Exit")) glfwSetWindowShouldClose(_window, true);
            ImGui::EndMenu();
        }
        ImGui::EndMainMenuBar();
    }

    ImGui::Begin("Controls");
    if (ImGui::BeginTabBar("MainTabBar")) {
        if (ImGui::BeginTabItem("PCAP")) { render_pcap_tab(); ImGui::EndTabItem(); }
        if (ImGui::BeginTabItem("Attack")) { render_attack_tab(); ImGui::EndTabItem(); }
        if (ImGui::BeginTabItem("Server")) { render_server_tab(); ImGui::EndTabItem(); }
        ImGui::EndTabBar();
    }
    ImGui::End();

    ImGui::Begin("Terminal");
    render_terminal_tab();
    ImGui::End();
}

void GuiManager::render_pcap_tab() {
    ImGui::Text("PCAP Capture");
    ImGui::Separator();

    if (_pcap_devices.empty()) {
        ImGui::Text("No capture devices found.");
        return;
    }

    std::vector<const char*> device_names;
    for (const auto& dev : _pcap_devices) device_names.push_back(dev.name.c_str());
    ImGui::Combo("Interface", &_selected_pcap_device, device_names.data(), device_names.size());
    ImGui::InputText("Output File", _pcap_output_filename, sizeof(_pcap_output_filename));

    if (_pcap_manager.is_capturing()) {
        if (ImGui::Button("Stop Capture")) {
            _pcap_manager.stop_capture();
            log_to_terminal("PCAP capture stopped.\n");
        }
    } else {
        if (ImGui::Button("Start Capture")) {
            try {
                _pcap_manager.start_capture(_pcap_devices[_selected_pcap_device].name, _pcap_output_filename);
                log_to_terminal("PCAP capture started on " + _pcap_devices[_selected_pcap_device].name + "\n");
            } catch (const std::exception& e) {
                log_to_terminal("Failed to start capture: " + std::string(e.what()) + "\n");
            }
        }
    }
}

void GuiManager::render_attack_tab() {
    ImGui::Text("Local Hashcat Attack");
    ImGui::Separator();

    ImGui::InputText("Hash File", _hash_file_buf, sizeof(_hash_file_buf));
    ImGui::InputText("Hash Mode (-m)", _hash_mode_buf, sizeof(_hash_mode_buf));
    ImGui::InputText("Attack Mode (-a)", _attack_mode_buf, sizeof(_attack_mode_buf));
    ImGui::InputText("Wordlist (optional)", _wordlist_buf, sizeof(_wordlist_buf));
    ImGui::InputText("Rules (optional)", _rules_buf, sizeof(_rules_buf));

    if (_hashcat_manager.is_attacking()) {
        ImGui::Text("Attack in progress...");
    } else {
        if (ImGui::Button("Start Local Attack")) {
            std::vector<std::string> args;
            args.push_back("-m");
            args.push_back(_hash_mode_buf);
            args.push_back("-a");
            args.push_back(_attack_mode_buf);
            args.push_back(_hash_file_buf);
            if (strlen(_wordlist_buf) > 0) args.push_back(_wordlist_buf);
            if (strlen(_rules_buf) > 0) { args.push_back("-r"); args.push_back(_rules_buf); }

            log_to_terminal("--- Starting local attack ---\n");
            _hashcat_manager.start_attack(args, [this](const std::string& out) {
                this->log_to_terminal(out);
            });
        }
    }
}

void GuiManager::render_server_tab() {
    ImGui::Text("Server Status");
    ImGui::Separator();
    ImGui::Text("Room ID: %s", _room_id.c_str());
    ImGui::Text("Scan the QR code with the mobile app to connect.");
    if (_qr_texture) {
        ImGui::Image((void*)(intptr_t)_qr_texture, ImVec2(256, 256));
    }
}

void GuiManager::render_terminal_tab() {
    std::lock_guard<std::mutex> lock(_terminal_mutex);
    ImGui::BeginChild("ScrollingRegion", ImVec2(0, 0), false, ImGuiWindowFlags_HorizontalScrollbar);
    ImGui::TextUnformatted(_terminal_buffer.begin(), _terminal_buffer.end());
    if (ImGui::GetScrollY() >= ImGui::GetScrollMaxY())
        ImGui::SetScrollHereY(1.0f);
    ImGui::EndChild();
}

void GuiManager::generate_qr_texture() {
    QRcode* qr = QRcode_encodeString(_room_id.c_str(), 0, QR_ECLEVEL_L, QR_MODE_8, 1);
    if (!qr) return;

    glGenTextures(1, &_qr_texture);
    glBindTexture(GL_TEXTURE_2D, _qr_texture);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

    // Convert QR code data to an RGBA texture
    std::vector<unsigned char> texture_data(qr->width * qr->width * 4);
    for (int y = 0; y < qr->width; y++) {
        for (int x = 0; x < qr->width; x++) {
            int index = y * qr->width + x;
            unsigned char color = (qr->data[index] & 1) ? 0 : 255;
            texture_data[index * 4 + 0] = color;
            texture_data[index * 4 + 1] = color;
            texture_data[index * 4 + 2] = color;
            texture_data[index * 4 + 3] = 255;
        }
    }

    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, qr->width, qr->width, 0, GL_RGBA, GL_UNSIGNED_BYTE, texture_data.data());
    QRcode_free(qr);
}