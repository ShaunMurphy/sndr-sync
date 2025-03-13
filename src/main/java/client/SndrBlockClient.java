package client;
import icelink.IceLink_Main;
import prototype.Request;
import prototype.tasks.Call;
import prototype.tasks.TaskManager;
import application.interfaces.ApiHelper;
import application.interfaces.Crypto;
import client.connection.ConnectionManager;
import client.connection.IceLinkConnectionFactory;
import client.connection.SndrSyncConnectionFactory;

import com.google.protobuf.GeneratedMessageV3;

public final class SndrBlockClient {
    private final ConnectionManager connectionManager;
    private final TaskManager taskManager;
    private final IceLink_Main icelink;
    
    public SndrBlockClient(IceLink_Main icelink, Crypto crypto, ApiHelper helper) {
        this.connectionManager = new ConnectionManager();
        this.connectionManager.registerFactory(new SndrSyncConnectionFactory());
        
        this.icelink = icelink;
        IceLinkConnectionFactory iceLinkFactory = new IceLinkConnectionFactory(this.icelink, helper, crypto);
        this.connectionManager.registerFactory(iceLinkFactory);
        
        this.taskManager = new TaskManager(this.connectionManager);
    }

    public final ConnectionManager getConnectionManager() {
        return this.connectionManager;
    }

    public <T extends GeneratedMessageV3> Call<T> sendRequest(Request request) {
        return this.taskManager.createCall(request);
    }

    public IceLink_Main getIceLink() {
        return this.icelink;
    }
}