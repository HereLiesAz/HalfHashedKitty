package hashkitty.java.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * A utility class for network-related operations.
 * <p>
 * This class is primarily used to discover the local IP address of the machine
 * running the Desktop Application. This IP address is encoded into the QR code
 * so the Android client knows where to connect.
 * </p>
 */
public class NetworkUtil {

    /**
     * Attempts to find the primary local IP address of the machine (LAN IP).
     * <p>
     * It iterates through all available network interfaces and their addresses,
     * looking for an IPv4 address that is:
     * 1. Not a loopback address (127.0.0.1).
     * 2. A site-local address (e.g., 192.168.x.x, 10.x.x.x).
     * </p>
     *
     * @return The local IP address as a String (e.g., "192.168.1.15"), or null if no suitable address is found.
     */
    public static String getLocalIpAddress() {
        try {
            // Get all network interfaces (eth0, wlan0, lo, etc.).
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface ni = networkInterfaces.nextElement();
                // Get all IP addresses associated with this interface.
                Enumeration<InetAddress> inetAddresses = ni.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress addr = inetAddresses.nextElement();
                    // Filter logic:
                    // !isLoopbackAddress(): Ignore 127.0.0.1.
                    // isSiteLocalAddress(): Checks if it's a private IP (RFC 1918).
                    // Regex matches standard IPv4 dotted-quad notation (excludes IPv6).
                    if (!addr.isLoopbackAddress()
                            && addr.isSiteLocalAddress()
                            && addr.getHostAddress().matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null; // Return null if no suitable address is found
    }
}
