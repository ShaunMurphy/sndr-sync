package server.connection;

import java.net.InetSocketAddress;

import util.Config.ConnectionType;

import common.connection.Connection;

interface ConnectionFactory {
    public ConnectionType getSupportedConnectionType();

    /**
     * This creates a connection.
     * @param manager
     * @param remoteDeviceUuid
     * @param remoteSocketAddress - The remote device that wants to connect to this device.
     * @return - the connection or null if failed.
     */
    public Connection createConnection(ConnectionManager manager, String remoteDeviceUuid, InetSocketAddress remoteSocketAddress);

    
    public Connection createConnection(ConnectionManager manager, String remoteDeviceUuid, String connectionUuid);
}