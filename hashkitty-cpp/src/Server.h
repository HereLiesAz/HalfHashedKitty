#ifndef SERVER_H
#define SERVER_H

#include <websocketpp/config/asio_no_tls.hpp>
#include <websocketpp/server.hpp>
#include <string>
#include <vector>
#include <map>
#include <set>
#include <functional>
#include <thread>
#include <mutex>

// Define the server and message types for convenience
typedef websocketpp::server<websocketpp::config::asio> server;
typedef websocketpp::connection_hdl connection_hdl;

// Struct to hold connection-specific data
struct ConnectionData {
    std::string room_id;
};

class WebSocketServer {
public:
    WebSocketServer();
    ~WebSocketServer();

    // Starts the server on the specified port in a background thread
    void run(uint16_t port);

    // Stops the server
    void stop();

    // Type definition for the attack callback
    using AttackCallback = std::function<void(const std::string&, connection_hdl)>;

    // Sets the callback function for handling attack commands
    void set_attack_callback(AttackCallback callback);

    // Sends a message to a specific client
    void send_message(connection_hdl hdl, const std::string& message);

private:
    void on_open(connection_hdl hdl);
    void on_close(connection_hdl hdl);
    void on_message(connection_hdl hdl, server::message_ptr msg);

    server _server;
    std::thread _server_thread;
    std::map<connection_hdl, ConnectionData, std::owner_less<connection_hdl>> _connections;
    // Correctly define the set with a custom comparator for weak_ptr (connection_hdl)
    std::map<std::string, std::set<connection_hdl, std::owner_less<connection_hdl>>> _rooms;
    std::mutex _mutex;
    AttackCallback _attack_callback;
};

#endif // SERVER_H