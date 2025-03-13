package server.connection;

import com.sndr.logger.SndrLogger;
import java.net.InetSocketAddress;

import ssdp.message.MessageGenerator;
import ssdp.message.SsdpConstants;
import util.Config.ConnectionType;
import common.ClientChannel;
import common.ServerChannel;
import common.connection.Connection;
import common.connection.Connection.State;
import common.connection.SndrSyncConnection;
import java.io.IOException;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;
import ssdp.message.SsdpUnicastSocket;

public final class SndrSyncConnectionFactory implements ConnectionFactory {

    private static final Logger LOGGER = SndrLogger.getLogger();

    @Override
    public ConnectionType getSupportedConnectionType() {
        return ConnectionType.SNDR_SYNC;
    }

    @SuppressWarnings("resource")
    @Override
    public Connection createConnection(ConnectionManager manager, String remoteDeviceUuid, InetSocketAddress remoteSocketAddress) {
        ServerChannel server;
        try {
            server = new ServerChannel(SsdpConstants.localAddress, 0);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Failed to create erver channel", ex);
            return null;
        }

        //Create a multicast SSDP Message to send.
        String searchTarget = SsdpConstants.ST_DEVICE();
        String location = server.getServerSocketAddress().toString();
        if (location.startsWith("/")) {
            location = location.substring(1);
        }

        String message = MessageGenerator.mSearchOK(searchTarget, location);
        //Open a socket for SSDP. Send the message.
        SsdpUnicastSocket socket = null;
        try {
            socket = new SsdpUnicastSocket(remoteSocketAddress.getAddress(), remoteSocketAddress.getPort());
        } catch (SocketException ex) {
            LOGGER.log(Level.SEVERE, "Failed to create ssdp unicast socket", ex);
        }

        if (null == socket) {
            return null;
        }

        try {
            socket.send(message, 0);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Failed to send unicast message", ex);
        } finally {
            socket.close();
        }

        //Accept the incoming connection from the client.
        boolean success = server.acceptServerChannelConnection();
        if (success) {
            final ClientChannel client = server.getClientChannel();
            SndrSyncConnection connection = new SndrSyncConnection(null, remoteDeviceUuid, client);
            connection.setState(State.CONNECTED);
            return connection;
        } else {
            return null;
        }
    }

    @Override
    public Connection createConnection(ConnectionManager manager, String remoteDeviceUuid, String connectionUuid) {
        throw new UnsupportedOperationException("Not supported");
    }
}