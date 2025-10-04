#include "Server.h"
#include <iostream>
#include "nlohmann/json.hpp" // Use the correct path for nlohmann/json

// Use nlohmann::json for convenience
using json = nlohmann::json;

WebSocketServer::WebSocketServer() {
    // Initialize Asio
    _server.init_asio();

    // Set up handlers
    _server.set_open_handler(bind(&WebSocketServer::on_open, this, std::placeholders::_1));
    _server.set_close_handler(bind(&WebSocketServer::on_close, this, std::placeholders::_1));
    _server.set_message_handler(bind(&WebSocketServer::on_message, this, std::placeholders::_1, std::placeholders::_2));
}

WebSocketServer::~WebSocketServer() {
    stop();
}

void WebSocketServer::run(uint16_t port) {
    _server.listen(port);
    _server.start_accept();
    _server_thread = std::thread([this, port]() {
        std::cout << "[*] WebSocket server starting on port " << port << std::endl;
        _server.run();
    });
}

void WebSocketServer::stop() {
    if (!_server.is_listening()) {
        return;
    }
    _server.stop_listening();
    // Close all connections
    for (auto const& [hdl, data] : _connections) {
        _server.close(hdl, websocketpp::close::status::going_away, "Server shutdown");
    }
    if (_server_thread.joinable()) {
        _server_thread.join();
    }
}

void WebSocketServer::set_attack_callback(AttackCallback callback) {
    _attack_callback = callback;
}

void WebSocketServer::on_open(connection_hdl hdl) {
    std::lock_guard<std::mutex> lock(_mutex);
    _connections[hdl] = ConnectionData();
    std::cout << "Client connected." << std::endl;
}

void WebSocketServer::on_close(connection_hdl hdl) {
    std::lock_guard<std::mutex> lock(_mutex);
    if (_connections.count(hdl)) {
        std::string room_id = _connections[hdl].room_id;
        if (!room_id.empty() && _rooms.count(room_id)) {
            _rooms[room_id].erase(hdl);
            if (_rooms[room_id].empty()) {
                _rooms.erase(room_id);
            }
        }
        _connections.erase(hdl);
    }
    std::cout << "Client disconnected." << std::endl;
}

void WebSocketServer::on_message(connection_hdl hdl, server::message_ptr msg) {
    std::lock_guard<std::mutex> lock(_mutex);

    try {
        auto j = json::parse(msg->get_payload());
        std::string type = j.value("type", "");

        if (type == "join") {
            std::string room_id = j.value("room_id", "");
            if (!room_id.empty()) {
                _connections[hdl].room_id = room_id;
                _rooms[room_id].insert(hdl);
                std::cout << "Client joined room: " << room_id << std::endl;
            }
        } else if (type == "attack") {
            if (_attack_callback) {
                // The payload for an attack is a stringified JSON object
                std::string attack_payload = j.value("payload", "{}");
                _attack_callback(attack_payload, hdl);
            }
        }

        // Broadcast the message to other clients in the same room
        std::string current_room = _connections[hdl].room_id;
        if (!current_room.empty() && _rooms.count(current_room)) {
            for (auto const& client_hdl : _rooms[current_room]) {
                if (client_hdl.lock() != hdl.lock()) { // Don't send back to the sender
                    _server.send(client_hdl, msg->get_payload(), msg->get_opcode());
                }
            }
        }
    } catch (json::parse_error& e) {
        std::cerr << "JSON parse error: " << e.what() << std::endl;
    }
}

void WebSocketServer::send_message(connection_hdl hdl, const std::string& message) {
    _server.send(hdl, message, websocketpp::frame::opcode::text);
}