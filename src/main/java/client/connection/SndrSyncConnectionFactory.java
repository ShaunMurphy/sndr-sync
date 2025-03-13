package client.connection;

import com.sndr.logger.SndrLogger;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;

import ssdp.message.MessageGenerator;
import ssdp.message.SsdpConstants;
import ssdp.message.SsdpMulticastSocket;
import ssdp.models.SsdpPacket;
import ssdp.util.NetworkUtil;
import util.Config.ConnectionType;

import common.ClientChannel;
import common.connection.Connection;
import common.connection.Connection.State;
import common.connection.SndrSyncConnection;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import util.Config;

public final class SndrSyncConnectionFactory implements ConnectionFactory {

    private static final Logger LOGGER = SndrLogger.getLogger();

    @Override
    public ConnectionType getSupportedConnectionType() {
        return ConnectionType.SNDR_SYNC;
    }

    @SuppressWarnings("resource")
    @Override
    public Connection createConnection(ConnectionManager manager, String remoteDeviceUuid) {
        //Create a multicast SSDP Message to send.
        String searchTarget = SsdpConstants.ST_DEVICE_UUID(remoteDeviceUuid);
        //String searchTarget = SsdpConstants.ST_DEVICE_TYPE_VERSION(SsdpConstants.DEVICE_TYPES.ALL, 1);
        String message = MessageGenerator.mSearchRequestMessage_MultiCast(2, searchTarget);
        DatagramPacket received = null;
        //Open a socket for SSDP. Send the message.
        SsdpMulticastSocket socket;
        try {
            socket = new SsdpMulticastSocket(NetworkUtil.getNetworkInterfaces(), 0);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Failed to create multicast socket", ex);
            return null;
        }

        try {
            socket.send(message, Config.DEFAULT_MULTICAST_RESPONSE_TIMEOUT);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Failed to send message", ex);
        }

        try {
            received = socket.receive();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Receive multicast timed out", ex);
        } finally {
            socket.close();
        }

        if (null == received) {
            return null;
        }

        //Convert the packet and get the location.
        SsdpPacket packet = new SsdpPacket(received);

        String location = packet.getData().location;
        String portString = location.substring(location.lastIndexOf(":") + 1);
        int port;
        try {
            port = Integer.valueOf(portString);
        } catch (NumberFormatException e) {
            LOGGER.log(Level.SEVERE, "Invalid port received", e);
            return null;
        }

        InetSocketAddress remoteSocketAddress = new InetSocketAddress(packet.getAddress(), port);
        //Try to connect to the server..
        ClientChannel channel = null;
        try {
            channel = new ClientChannel(remoteSocketAddress, Config.DEFAULT_CONNECT_TIMEOUT);
        } catch (IOException ex) {
           LOGGER.log(Level.SEVERE, "Failed to create client channel", ex);
           return null;
        }

        SndrSyncConnection connection = new SndrSyncConnection(manager, remoteDeviceUuid, channel);
        connection.setState(State.CONNECTED);

        return connection.isConnected() ? connection : null;
    }
}