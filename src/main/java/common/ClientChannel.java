package common;

import com.sndr.logger.SndrLogger;
import util.Config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ClientChannel implements AutoCloseable {

    private static final Logger LOGGER = SndrLogger.getLogger();

    private final SocketChannel clientChannel;
    private InputStream inputStream;
    private OutputStream outputStream;

    public ClientChannel(final InetSocketAddress remoteSocket, int timeout) throws IOException {
        clientChannel = SocketChannel.open();
        LOGGER.log(Level.INFO, "Waiting {0} seconds to connect to the server", Config.CLIENT_SOCKET_TIMEOUT);
        // Wait for the connection.
        Socket socket = clientChannel.socket();
        // Set read timeout
        socket.setSoTimeout(Config.CLIENT_SOCKET_TIMEOUT * 1000);
        // Ensure the connection attempt times out
        socket.connect(remoteSocket, timeout);
        boolean connected = clientChannel.isConnected();
        LOGGER.log(connected ? Level.INFO : Level.WARNING, connected ? "Connected to server at "+remoteSocket.getHostString() : "Failed to connect to the server.");
    }

    /**
     * Sets the client SocketChannel directly. Should only be called by {@link ServerChannel}.
     * @param clientChannel
     */
    ClientChannel(SocketChannel clientChannel) {
        this.clientChannel = clientChannel;
    }
    
    @Override
    public void close() {
        if(this.clientChannel != null) {
            try {
                this.clientChannel.close();
            } catch (IOException e) {
                LOGGER.log(Level.INFO, "Failed to close client channel");
            }
        }
    }

    public final InputStream getInputStream() throws IOException {
        if(this.inputStream == null) {
            this.inputStream = clientChannel.socket().getInputStream();
        }
        return this.inputStream;
    }
    
    public final OutputStream getOutputStream() throws IOException {
        if(this.outputStream == null) {
            this.outputStream = clientChannel.socket().getOutputStream();
        }
        return this.outputStream;
    }

    public final SocketChannel getClientChannel() {
        return this.clientChannel;
    }
}