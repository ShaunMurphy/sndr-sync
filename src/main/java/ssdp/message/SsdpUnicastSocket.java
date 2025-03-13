package ssdp.message;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;

/**
 * Creates a Unicast socket. Used to send and receive packets.
 * Remember to close this class when finished. Closing this class will close the
 * socket.
 *
 * @author shaun
 *
 */
public class SsdpUnicastSocket extends AbstractSsdpSocket<DatagramSocket> {

    private InetSocketAddress remoteSocket = null;

    public SsdpUnicastSocket(InetAddress address, int port) throws SocketException {
        super(new DatagramSocket(null));
        socket.setReuseAddress(true);
        socket.bind(new InetSocketAddress(port));
        remoteSocket = new InetSocketAddress(address, port);
    }

    /**
     * Sends a message, waits for a response for the timeout duration.
     *
     * @param message
     * @param timeout (optional) - time in seconds. With this option set to a
     * non-zero timeout,
     * a call to receive() for this DatagramSocket will block for only this
     * amount of time.
     * @throws java.net.SocketException
     * @throws java.io.IOException
     */
    public final void send(String message, int timeout) throws SocketException, IOException {
        super.send(message, remoteSocket, timeout);
    }
}