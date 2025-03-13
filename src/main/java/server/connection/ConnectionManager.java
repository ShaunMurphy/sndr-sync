package server.connection;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import server.IceLinkServer;
import server.Server;
import util.Config;
import util.Config.ConnectionType;

import common.connection.Connection;
import common.connection.IceLinkConnection;
import common.connection.SndrSyncConnection;
import common.manager.CoreManager;

public final class ConnectionManager {
    private final List<ConnectionFactory> factories = new ArrayList<>(2);
    private static final HashMap<String, Connection> connectionMap = new HashMap<>();
    private final Server server;
    private final IceLinkServer icelinkServer;
    public ConnectionManager(Server server, IceLinkServer icelinkServer) {
        this.server = server;
        this.icelinkServer = icelinkServer;
        new ConnectionMonitor().start(5, TimeUnit.MINUTES);
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
    
    public final void createConnection(ConnectionType type, String deviceUuid, InetSocketAddress socketAddress) {
        ConnectionFactory factory = getFactory(type);
        Connection connection = factory.createConnection(this, deviceUuid, socketAddress);
        if(connection instanceof SndrSyncConnection) {
            this.server.registerSocketChannel(((SndrSyncConnection)connection).getChannel());
        }
        putConnection(type, deviceUuid, connection);
    }

    public final void createConnection(ConnectionType type, String deviceUuid, String connectionUuid) {
        ConnectionFactory factory = getFactory(type);
        Connection connection = factory.createConnection(this, deviceUuid, connectionUuid);
        if(connection instanceof IceLinkConnection) {
            IceLinkConnection ilc = (IceLinkConnection)connection;
            System.out.println("Set protobuf listener!");
            ilc.getRemoteConnection().setProtobufListener(icelinkServer);
        }
        putConnection(type, deviceUuid, connection);
    }


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
        //Throw unsupported exception?
        throw new UnsupportedOperationException("The factory for type "+type.name()+" was not registered.");
    }

    //Prototype for checking if connections are expired.
    private final class ConnectionMonitor {
        private final ScheduledExecutorService executor;
        private ScheduledFuture<?> future;
        ConnectionMonitor() {
            ThreadFactory factory = CoreManager.createThreadFactory("Connection Monitor: ");
            executor = new ScheduledThreadPoolExecutor(1, factory);
        }
        private final void start(long delay, TimeUnit unit) {
            final Runnable command = new Runnable() {
                @Override
                public void run() {
                    if(connectionMap.isEmpty()) {
                        return;
                    }
                    
                    Iterator<Connection> iterator = connectionMap.values().iterator();
                    long now = System.currentTimeMillis();
                    while(iterator.hasNext()) {
                        Connection connection = iterator.next();
                        //Maybe this needs to check if the connection is in use,
                        //if so, skip it.
                        
                        //Determine if the connection is expired.
                        int timeout = Config.INACTIVE_CONNECTION_TIME(connection.getConnectionType());                        
                        if(now > timeout + connection.getLastContacted()) {
                            System.out.println("Removed old connection "+connection.getConnectionType().name()+" "+connection.getRemoteDeviceUuid());
                            //TODO If this is a SndrSync connection it needs to remove it from the Server!
                            iterator.remove();
                        }
                    }
                }
            };
           this.future = executor.scheduleWithFixedDelay(command, delay, delay, unit);
        }
        
        private final void stop() {
            if(executor.isShutdown()) {
                return;
            }
            
            if(future != null) {
                future.cancel(true);
            }

            executor.shutdown();
            try {
                executor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            executor.shutdownNow();
        }
    }
}