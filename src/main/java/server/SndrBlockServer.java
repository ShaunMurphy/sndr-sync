package server;

import icelink.IceLink_Main;

import java.net.InetSocketAddress;

import server.connection.ConnectionManager;
import server.connection.IceLinkConnectionFactory;
import server.connection.SndrSyncConnectionFactory;
import ssdp.CreateConnectionHandler;
import ssdp.models.SsdpPacket;
import util.Config.ConnectionType;
import application.interfaces.ApiHelper;
import application.interfaces.Crypto;

public final class SndrBlockServer {
    private final ConnectionManager connectionManager;
    private Server server = null;
    private IceLinkServer icelinkServer = null;
    //private final Crypto crypto;
    //private final ApiHelper helper;
    //private final DatabaseMonitor dbMonitor;
    private final IceLink_Main icelink;

    public SndrBlockServer(IceLink_Main icelink, Crypto crypto, ApiHelper helper) {
        this.server = new Server();
        this.icelinkServer = new IceLinkServer();
        //this.crypto = crypto;
        //this.helper = helper;
        this.connectionManager = new ConnectionManager(this.server, this.icelinkServer);
        this.connectionManager.registerFactory(new SndrSyncConnectionFactory());
        this.icelink = icelink;
        IceLinkConnectionFactory icelinkFactory = new IceLinkConnectionFactory(icelink, helper, crypto);
        this.connectionManager.registerFactory(icelinkFactory);

        new Controller(crypto, helper);
        Processor.INSTANCE.register(Controller.class);

        //TODO need a better way to do this. Maybe an interface?
        this.icelink.setServerConnectionManager(this.connectionManager);
        
        //Database.get();
        //this.dbMonitor = new DatabaseMonitor(Config.SERVER_ROOT_DIRECTORY().toPath());
        //this.dbMonitor.rebuildDatabaseFileTree();
    }
    
    public final void shutdownServer() {
        this.server.shutdown();
    }
    
    public final ConnectionManager getConnectionManager() {
        return this.connectionManager;
    }
    
    //TODO Maybe this needs to go on a background thread?
    /**
     * This gets called when a SSDP packet matching this device is received.
     */
    private final CreateConnectionHandler handler = new CreateConnectionHandler() {
        @Override
        public void createConnection(SsdpPacket.Data data) {
            //This is data from the client's request.
            String remoteDeviceUuid = data.CPUUID;
            //TODO Reject this if the remoteDeviceUuid is null, empty, or does not belong to this user.
            if(remoteDeviceUuid == null || remoteDeviceUuid.isEmpty()) {
                System.out.println("Ignoring packet, CPUUID is missing.");
                return;
            }
            InetSocketAddress socketAddress = new InetSocketAddress(data.remoteAddress, data.remotePort);
            connectionManager.createConnection(ConnectionType.SNDR_SYNC, remoteDeviceUuid, socketAddress);
        }
    };
    
    public final CreateConnectionHandler getCreateConnectionHandler() {
        return this.handler;
    }
    
    public final void connectionReady(String connectionUuid, String deviceUuid) {
        this.connectionManager.createConnection(ConnectionType.ICELINK, deviceUuid, connectionUuid);
    }
}