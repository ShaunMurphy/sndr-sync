package ssdp.message;

import com.sndr.logger.SndrLogger;
import ssdp.util.NetworkUtil;
import util.Config;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Creates a Unicast or Multicast socket. Used to send and receive packets.
 * Remember to close this class when finished. Closing this class will close the
 * socket.
 *
 * @author shaun
 *
 */
public class SsdpMulticastSocket extends AbstractSsdpSocket<MulticastSocket> {

    private enum GroupOperation {
        JOIN,
        LEAVE
    }

    private static final Logger LOGGER = SndrLogger.getLogger();

    private final List<NetworkInterface> interfaces;

    public SsdpMulticastSocket(NetworkInterface networkInterface, int port) throws SocketException, IOException {
        this(Collections.singletonList(networkInterface), port);
    }

    /**
     * The address and port are local.<br>
     *
     * @param networkInterfaces
     * @param port
     * @throws java.net.SocketException
     * @throws java.io.IOException
     * @throws java.lang.IllegalArgumentException
     */
    public SsdpMulticastSocket(List<NetworkInterface> networkInterfaces, int port) throws SocketException, IOException {
        //Do not use the empty constructor, Android's MulticastSocket is different.
        super(new MulticastSocket(null));
        this.interfaces = networkInterfaces;
        //multicastSocket.setReuseAddress(true);//This is done internally.
        socket.setTimeToLive(Config.MULTICAST_TTL);
        socket.setLoopbackMode(Config.DISABLE_MULTICAST_LOOPBACK);//Ignores packets from the same ip address.
        joinGroups(networkInterfaces);
    }

    /**
     * Sends a Multicast Message.
     *
     * @param message
     * @param timeout (optional) - time in seconds. With this option set to a
     * non-zero timeout,
     * a call to receive() for this DatagramSocket will block for only this
     * amount of time.
     * @throws java.net.SocketException
     * @throws IOException
     */
    public final void send(String message, int timeout) throws SocketException, IOException {
        send(message, NetworkUtil.getInetAddresses(interfaces), timeout);
    }

    @Override
    public void close() {
        leaveGroups(interfaces);
        super.close();
    }

    /**
     * Sends the message on the list of address to both v4 and v6 multicast
     * groups
     *
     * @param message the message to send
     * @param addresses the addresses to send on
     * @param timeout the timeout of the send request
     * @throws SocketException
     * @throws IOException
     */
    private void send(String message, List<InetAddress> addresses, int timeout) throws SocketException, IOException {
        for (InetAddress address : addresses) {
            //Fix for MAC.
            if (SsdpConstants.deviceOS.equals(SsdpConstants.OS.Mac)) {
                NetworkInterface networkInterface = NetworkInterface.getByInetAddress(address);
                if (networkInterface != null) {
                    socket.setNetworkInterface(networkInterface);
                }
            }

            if (address instanceof Inet4Address) {
                super.send(message, SsdpConstants.multicastIPv4Socket, timeout);
            } else if (address instanceof Inet6Address) {
                super.send(message, SsdpConstants.multicastIPv6Socket, timeout);
            } else {
                LOGGER.log(Level.WARNING, "Unexpected InetAddress {0} to send on", address);
            }
        }
    }

    /**
     * Attempts to join the v4 and v6 multicast group on the given interfaces.
     *
     * @param interfaces
     */
    private void joinGroups(List<NetworkInterface> interfaces) {
        performGroupOperation(interfaces, GroupOperation.JOIN);
    }

    /**
     * Attempts to leave the v4 and v6 multicast group on the given interfaces
     *
     * @param interfaces
     */
    private void leaveGroups(List<NetworkInterface> interfaces) {
        performGroupOperation(interfaces, GroupOperation.LEAVE);
    }

    /**
     * Attempts to perform the {@link GroupOperation} for the v4 and v6
     * multicast group on the given interfaces
     *
     * @param interfaces
     */
    private void performGroupOperation(List<NetworkInterface> interfaces, GroupOperation operation) {
        for (NetworkInterface i : interfaces) {
            Enumeration<InetAddress> elements = i.getInetAddresses();
            while (elements.hasMoreElements()) {
                InetAddress address = elements.nextElement();
                try {
                    if (address instanceof Inet4Address) {
                        performOperation(SsdpConstants.multicastIPv4Socket, i, operation);
                    } else if (address instanceof Inet6Address) {
                        performOperation(SsdpConstants.multicastIPv6Socket, i, operation);
                    } else {
                        LOGGER.log(Level.WARNING, "Unexpected InetAddress {0}", address);
                    }
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, "Failed to leave the multicast socket group", ex);
                }
            }
        }
    }

    /**
     * Performs the group operation for the multicast address and interface
     *
     * @param address the multicast adsress
     * @param networkInterface the interface
     * @param operation the operation to perform (leave/join)
     * @throws IOException
     */
    private void performOperation(InetSocketAddress address, NetworkInterface networkInterface, GroupOperation operation) throws IOException {
        if (null == operation) {
            throw new IllegalArgumentException("Invalid GroupOperation");
        }

        switch (operation) {
            case JOIN:
                socket.joinGroup(address, networkInterface);
                break;
            case LEAVE:
                socket.leaveGroup(address, networkInterface);
                break;
            default:
                LOGGER.log(Level.WARNING, "Unexpected GroupOperation {0}", operation);
                break;
        }
    }
}