package hashkitty.java.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * A utility class for network-related operations.
 */
public class NetworkUtil {

    /**
     * Attempts to find the primary local IP address of the machine.
     * It prioritizes non-loopback, site-local IPv4 addresses.
     *
     * @return The local IP address as a String, or null if not found.
     */
    public static String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface ni = networkInterfaces.nextElement();
                Enumeration<InetAddress> inetAddresses = ni.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress addr = inetAddresses.nextElement();
                    // Check for a non-loopback, site-local IPv4 address
                    if (!addr.isLoopbackAddress() && addr.isSiteLocalAddress() && addr.getHostAddress().matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")) {
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