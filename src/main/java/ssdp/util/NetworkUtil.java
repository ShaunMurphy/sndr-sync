package ssdp.util;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public final class NetworkUtil {
    private NetworkUtil() {}
    //TODO How to determine which index to return?
    /**
     * Returns the first local IPv4 IP Address.
     * @return
     */
    public static InetAddress getIPv4Address() {
        List<InetAddress> list = getLocalAddress(true, false, null);
        return list.isEmpty() ? null : list.get(0);
    }
   
    public static InetAddress getAndroidWIfiIPv4Address() {
        List<InetAddress> list = getLocalAddress(true, false, "wlan0");
        return list.isEmpty() ? null : list.get(0);        
    }
    
    //TODO How to determine which index to return?
    /**
     * Returns the first local IPv6 IP Address.
     * @return
     */
    public static InetAddress getIPv6Address() {
        List<InetAddress> list = getLocalAddress(false, true, null);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * Return IP addresses on the system. Ignores loopback addresses.
     * @param getIPv4
     * @param getIPv6
     * @param interfaceName - Optional, if not null, this will only filter on the interface with that name.
     * @return
     */
    private static List<InetAddress> getLocalAddress(boolean getIPv4, boolean getIPv6, String interfaceName)  {
        List<InetAddress> list = new ArrayList<>();
        Enumeration<NetworkInterface> interfaces = null;
        try {
            interfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        while(interfaces.hasMoreElements()) {
            NetworkInterface i = interfaces.nextElement();
            if(interfaceName != null && !i.getName().equals(interfaceName)) {
                continue;
            }
            Enumeration<InetAddress> addresses = i.getInetAddresses();
            while(addresses.hasMoreElements()) {
                InetAddress address = addresses.nextElement();
                if(address.isLoopbackAddress()) {
                    continue;
                }

                if(!getIPv6 && address instanceof Inet6Address) {
                    continue;
                } else if(!getIPv4 && address instanceof Inet4Address) {
                    continue;
                } else {
                    list.add(address);
                }
            }
        }
        return list;
    }

    //This may return blue tooth interfaces too.
    /**
     * Returns all network interfaces of the system that are passed by the filter in {@link #acceptInterface(NetworkInterface)}.
     * @return
     */
    public static List<NetworkInterface> getNetworkInterfaces() {
        List<NetworkInterface> list = new ArrayList<>();
        Enumeration<NetworkInterface> interfaces = null;
        try {
            interfaces = NetworkInterface.getNetworkInterfaces();
            while(interfaces.hasMoreElements()) {
                NetworkInterface i = interfaces.nextElement();
                if(acceptInterface(i)) {
                    list.add(i);
                } else {
                    //System.out.println("Ignored interface "+i.getDisplayName());
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return list;
    }
    
    /**
     * Sets the conditions for the {@link #getNetworkInterfaces()} to accept the NetworkInterface on its return list.
     * @param i
     * @return
     * @throws SocketException
     */
    private static boolean acceptInterface(NetworkInterface i) throws SocketException {
        return i.isUp() && i.supportsMulticast() && !i.isLoopback() && !i.getInterfaceAddresses().isEmpty();
    }

    /**
     * Returns a list of InetAddresses from a list of NetworkInterfaces.
     * @param interfaces
     * @return
     */
    public static List<InetAddress> getInetAddresses(List<NetworkInterface> interfaces) {
        List<InetAddress> addresses = new ArrayList<>(interfaces.size());
        for(NetworkInterface i : interfaces) {
            Enumeration<InetAddress> elements = i.getInetAddresses();
            while(elements.hasMoreElements()) {
                InetAddress address = elements.nextElement();
                addresses.add(address);
            }
        }
        return addresses;
    }
}