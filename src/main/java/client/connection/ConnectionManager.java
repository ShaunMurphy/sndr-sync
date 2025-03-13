package client.connection;

import fm.icelink.DataBuffer;
import icelink.RemoteConnection;
import icelink.RemoteConnection.Header;
import icelink.RemoteConnection.ReceiveListener;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sndr.logger.SndrLogger;

import util.Config.ConnectionType;
import common.connection.Connection;
import common.connection.ConnectionStateListener;
import common.connection.IceLinkConnection;

public final class ConnectionManager implements ConnectionStateListener {

    private final Logger logger = SndrLogger.getLogger();
    private final List<ConnectionFactory> factories = new ArrayList<>(2);
    private static final HashMap<String, Connection> connectionMap = new HashMap<>();
    
    public ConnectionManager() {
    }
    
    @Override
    public void onStateChanged(Connection connection, Connection.State oldState) {
        if (Connection.State.CLOSED.equals(connection.getState())) {
            connectionMap.remove(connection.getConnectionType().name() + connection.getRemoteDeviceUuid());
        }
    }

    public final void registerFactory(ConnectionFactory factory) {
        if(!factories.contains(factory)) {
            factories.add(factory);
        }
    }
    
    public final boolean unregisterFactory(ConnectionFactory factory) {
        return factories.remove(factory);
    }

    public final Connection getConnection(ConnectionType type, String deviceUuid) {
        //TODO Maybe this needs to mark the connection as its being used?
        //TODO What happens when multiple threads concurrently call this?
        Connection connection = connectionMap.get(type.name()+deviceUuid);
        return connection;
    }

    public final void createConnection(ConnectionType type, String deviceUuid) {
        ConnectionFactory factory = getFactory(type);
        System.out.println("Creating connection for type "+type.name()+" for deviceUuid "+deviceUuid);
        Connection connection = factory.createConnection(this, deviceUuid);
        
        if(connection instanceof IceLinkConnection) {
            ((IceLinkConnection)connection).getRemoteConnection().setProtobufListener(l);
        }
        
        putConnection(type, deviceUuid, connection);
    }

    //TODO REMOVE!
    private final ReceiveListener l = new ReceiveListener() {

        @Override
        public void receiveString(RemoteConnection connection, String message) {
            logger.log(Level.FINE, message);
        }

        @Override
        public void receiveDataBuffer(RemoteConnection connection, DataBuffer data) {
            logger.log(Level.FINE, "buffer length "+data.getLength());
            
        }

        @Override
        public void receiveProtobuf(RemoteConnection connection, Header header, DataBuffer data) {
            logger.log(Level.FINE, header.type.name()+" "+data.getLength());
            
        }

        @Override
        public void receiveProtobuf(RemoteConnection connection, ByteArrayInputStream bais) {
            // TODO Auto-generated method stub
            
        }        
    };
    
    
    /**
     * Adds the connection to the connection map.
     * @param type
     * @param deviceUuid
     * @param connection
     * @return - the old connection from the map if it exists.
     */
    private final Connection putConnection(ConnectionType type, String deviceUuid, Connection connection) {
        Connection oldConnection = connectionMap.get(type.name()+deviceUuid);
        connectionMap.put(type.name()+deviceUuid, connection);
        //TODO Does this need to make sure that the connection does not already exist in the map?
        //Does this need to allow multiple connections for the same device?
        return oldConnection;
    }

    private final ConnectionFactory getFactory(ConnectionType type) {
        for(ConnectionFactory factory : factories) {
            if(factory.getSupportedConnectionType().equals(type)) {
                return factory;
            }
        }
        throw new UnsupportedOperationException("The factory for type "+type.name()+" was not registered.");
    }
}