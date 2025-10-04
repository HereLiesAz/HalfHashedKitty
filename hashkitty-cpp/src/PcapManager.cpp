#include "PcapManager.h"
#include <iostream>

PcapManager::PcapManager() : _handle(nullptr), _dumper(nullptr), _is_capturing(false) {}

PcapManager::~PcapManager() {
    if (_is_capturing) {
        stop_capture();
    }
}

std::vector<PcapDevice> PcapManager::get_all_devs() {
    pcap_if_t* alldevs;
    std::vector<PcapDevice> devices;

    if (pcap_findalldevs(&alldevs, _errbuf) == -1) {
        throw std::runtime_error("Error in pcap_findalldevs: " + std::string(_errbuf));
    }

    for (pcap_if_t* d = alldevs; d; d = d->next) {
        PcapDevice dev;
        dev.name = d->name;
        dev.description = (d->description) ? d->description : "No description available";
        devices.push_back(dev);
    }

    pcap_freealldevs(alldevs);
    return devices;
}

void PcapManager::start_capture(const std::string& interface_name, const std::string& output_filename) {
    if (_is_capturing) {
        throw std::runtime_error("Capture is already in progress.");
    }

    _handle = pcap_open_live(interface_name.c_str(), BUFSIZ, 1, 1000, _errbuf);
    if (_handle == nullptr) {
        throw std::runtime_error("pcap_open_live() failed: " + std::string(_errbuf));
    }

    _dumper = pcap_dump_open(_handle, output_filename.c_str());
    if (_dumper == nullptr) {
        pcap_close(_handle);
        _handle = nullptr;
        throw std::runtime_error("pcap_dump_open() failed: " + std::string(pcap_geterr(_handle)));
    }

    _is_capturing = true;
    _capture_thread = std::thread(&PcapManager::capture_loop, this);
}

void PcapManager::capture_loop() {
    // pcap_loop will block until pcap_breakloop is called or an error occurs.
    // The third argument is a callback, which we provide as a lambda.
    pcap_loop(_handle, -1, [](u_char *user, const struct pcap_pkthdr *header, const u_char *bytes) {
        pcap_dump(user, header, bytes);
    }, reinterpret_cast<u_char*>(_dumper));

    // Cleanup after the loop is broken
    pcap_close(_handle);
    pcap_dump_close(_dumper);
    _handle = nullptr;
    _dumper = nullptr;
}

void PcapManager::stop_capture() {
    if (!_is_capturing || !_handle) {
        return;
    }

    _is_capturing = false;
    if (_handle) {
        pcap_breakloop(_handle);
    }

    if (_capture_thread.joinable()) {
        _capture_thread.join();
    }

    std::cout << "Capture stopped." << std::endl;
}

bool PcapManager::is_capturing() const {
    return _is_capturing;
}