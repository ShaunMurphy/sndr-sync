package ssdp.message;

import com.sndr.logger.SndrLogger;
import util.Config;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class for Creates a socket. Used to send and receive packets.
 * Remember to close this class when finished. Closing this class will close the
 * socket.
 *
 * @author shaun
 * @param <T>
 *
 */
public abstract class AbstractSsdpSocket<T extends DatagramSocket> {

    private static final Logger LOGGER = SndrLogger.getLogger();

    private final boolean showIncomingTransactionIpAddresses = false;
    private final boolean showOutgoingTransactionIpAddresses = false;

    private final ByteBuffer buffer = ByteBuffer.allocate(2048);

    private boolean blocking = false;
    private int timeout = Config.DEFAULT_MULTICAST_RESPONSE_TIMEOUT;

    protected final T socket;

    public AbstractSsdpSocket(T socket) {
        this.socket = socket;
    }

    /**
     * Sends a message, waits for a response for the timeout duration.
     *
     * @param message
     * @param remoteSocket
     * @param timeout (optional) - time in seconds. With this option set to a
     * non-zero timeout,
     * a call to receive() for this DatagramSocket will block for only this
     * amount of time.
     * @throws java.net.SocketException
     * @throws java.io.IOException
     */
    protected void send(String message, InetSocketAddress remoteSocket, int timeout) throws SocketException, IOException {
        socket.setSoTimeout(timeout * 1000);
        DatagramPacket packet = new DatagramPacket(message.getBytes("UTF-8"), message.length(), remoteSocket);
        if (showOutgoingTransactionIpAddresses) {
            LOGGER.log(Level.INFO, "Send packet using: {0} ==> {1}", new Object[]{remoteSocket, packet.getSocketAddress()});
        }
        socket.send(packet);
    }

    /**
     * This method blocks until a datagram is received.
     *
     * @return
     * @throws SocketTimeoutException
     */
    public DatagramPacket receive() throws SocketTimeoutException, IOException {
        buffer.clear();

        if (!blocking) {
            socket.setSoTimeout(timeout);
        }

        DatagramPacket packet = new DatagramPacket(buffer.array(), buffer.capacity());
        socket.receive(packet);
        if (showIncomingTransactionIpAddresses) {
            LOGGER.log(Level.INFO, "Received packet {0} <== {1} | {2}",
                    new Object[]{socket.getLocalSocketAddress(),
                        packet.getSocketAddress(),
                        socket.getRemoteSocketAddress()
                    });
        }
        return packet;
    }

    /**
     * Close the socket
     */
    public void close() {
        if (socket == null) {
            return;
        }

        if (socket.isClosed()) {
            return;
        }

        socket.close();
    }

    /**
     * Sets the timeout on socket when receiving and not blocking
     *
     * @param timeout the timeout to set
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * Sets whether to a timeout it set on the socket when receiving
     *
     * @param blocking the blocking flag to set
     */
    public void setBlocking(boolean blocking) {
        this.blocking = blocking;
    }

    /**
     * This returns the socket address for multicast or unicast. Depending on
     * the parameter passed into the constructor.
     *
     * @return
     */
    public final String getSocketAddress() {
        return socket != null ? socket.getLocalSocketAddress().toString() : null;
    }

    public final int getPort() {
        return socket != null ? socket.getLocalPort() : -1;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + "datagramSocket=" + (null != socket ? socket.getLocalAddress() : "null")
                + ":" + (null != socket ? socket.getLocalPort() : "null") + ", remoteSocket=" + socket + '}';
    }
}