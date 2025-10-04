#ifndef PCAP_MANAGER_H
#define PCAP_MANAGER_H

#include <pcap.h>
#include <string>
#include <vector>
#include <stdexcept>
#include <thread>
#include <atomic>

// A simple struct to hold device information
struct PcapDevice {
    std::string name;
    std::string description;
};

class PcapManager {
public:
    PcapManager();
    ~PcapManager();

    // Gets a list of all available network devices
    std::vector<PcapDevice> get_all_devs();

    // Starts capturing packets on a given interface, saving to a file
    void start_capture(const std::string& interface_name, const std::string& output_filename);

    // Stops the current packet capture
    void stop_capture();

    // Checks if a capture is currently running
    bool is_capturing() const;

private:
    void capture_loop();

    pcap_t* _handle;
    pcap_dumper_t* _dumper;
    std::thread _capture_thread;
    std::atomic<bool> _is_capturing;
    char _errbuf[PCAP_ERRBUF_SIZE];
};

#endif // PCAP_MANAGER_H