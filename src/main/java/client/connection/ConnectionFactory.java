package client.connection;

import util.Config.ConnectionType;

import common.connection.Connection;

interface ConnectionFactory {
    public ConnectionType getSupportedConnectionType();

    /**
     * This creates a connection.
     * @param manager
     * @param remoteDeviceUuid - The remote device you want to connection.
     * @return - the connection or null if failed.
     */
    public Connection createConnection(ConnectionManager manager, String remoteDeviceUuid);
}