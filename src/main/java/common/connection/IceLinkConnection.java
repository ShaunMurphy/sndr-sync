package common.connection;

import icelink.RemoteConnection;
import util.Config.ConnectionType;

public class IceLinkConnection implements Connection {
    private final ConnectionStateListener listener;
    private final String remoteDeviceUuid;
    private String connectionUuid;
    private long lastContacted = -1;//TODO This needs to be updatable!
    private final RemoteConnection connection;
    private State state = State.INITIAL;

    public IceLinkConnection(ConnectionStateListener listener, final String remoteDeviceUuid) {
        this.listener = listener;
        this.remoteDeviceUuid = remoteDeviceUuid;
        this.lastContacted = System.currentTimeMillis();
        
        this.connection = new RemoteConnection();
    }
    
    @Override
    public final String getRemoteDeviceUuid() {
        return this.remoteDeviceUuid;
    }

    @Override
    public final ConnectionType getConnectionType() {
        return ConnectionType.ICELINK;
    }
    
    @Override
    public boolean isConnected() {
        return false;
    }


    @Override
    public void close() {
        setState(State.CLOSING);
        connection.close();
        setState(State.CLOSED);
    }

    @Override
    public long getLastContacted() {
        return this.lastContacted;
    }

    public final RemoteConnection getRemoteConnection() {
        return this.connection;
    }

    @Override
    public State getState() {
        return this.state;
    }

    @Override
    public void setState(State state) {
        State old = this.state;
        this.state = state;
        if (null != listener) {
            listener.onStateChanged(this, old);
        }
    }
    
    public String getConnectionUuid() {
        return this.connectionUuid;
    }
}