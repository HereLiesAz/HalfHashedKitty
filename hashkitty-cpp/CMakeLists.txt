cmake_minimum_required(VERSION 3.10)
project(HashKittyCpp)

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED True)

# --- Find System Dependencies ---
find_package(Threads REQUIRED)
find_package(Boost 1.71.0 REQUIRED COMPONENTS system thread)
find_package(OpenGL REQUIRED)
find_package(GLEW REQUIRED)
find_package(glfw3 REQUIRED)
find_package(PkgConfig REQUIRED)
pkg_check_modules(QRENCODE REQUIRED libqrencode)

# Find libpcap manually for better portability
find_path(PCAP_INCLUDE_DIR pcap.h)
find_library(PCAP_LIBRARY NAMES pcap)
if(NOT PCAP_INCLUDE_DIR OR NOT PCAP_LIBRARY)
    message(FATAL_ERROR "libpcap not found. Please install libpcap-dev or equivalent.")
endif()

# --- Add Source Files ---
file(GLOB IMGUI_SOURCES
    "vendor/imgui/*.cpp"
    "vendor/imgui/backends/imgui_impl_glfw.cpp"
    "vendor/imgui/backends/imgui_impl_opengl3.cpp"
)

add_executable(hashkitty_desktop
    main.cpp
    src/PcapManager.cpp
    src/HashcatManager.cpp
    src/Server.cpp
    src/GuiManager.cpp
    ${IMGUI_SOURCES}
)

# --- Include Directories ---
target_include_directories(hashkitty_desktop PRIVATE
    ${PCAP_INCLUDE_DIR}
    ${Boost_INCLUDE_DIRS}
    ${OPENGL_INCLUDE_DIR}
    ${GLEW_INCLUDE_DIRS}
    ${QRENCODE_INCLUDE_DIRS}
    ${CMAKE_CURRENT_SOURCE_DIR}/src
    ${CMAKE_CURRENT_SOURCE_DIR}/vendor/json/single_include
    ${CMAKE_CURRENT_SOURCE_DIR}/vendor/websocketpp
    ${CMAKE_CURRENT_SOURCE_DIR}/vendor/imgui
    ${CMAKE_CURRENT_SOURCE_DIR}/vendor/imgui/backends
)

# --- Link Libraries ---
target_link_libraries(hashkitty_desktop
    PRIVATE
    Threads::Threads
    Boost::system
    Boost::thread
    ${PCAP_LIBRARY}
    ${OPENGL_LIBRARIES}
    ${GLEW_LIBRARIES}
    ${QRENCODE_LIBRARIES}
    glfw
    stdc++fs
)