package common.connection;

import util.Config.ConnectionType;

import common.ClientChannel;

public final class SndrSyncConnection implements Connection {
    private final ConnectionStateListener listener;
    private final String remoteDeviceUuid;
    private final ClientChannel channel;
    private State state = State.INITIAL;
    private long lastContacted = -1;//TODO This needs to be updatable!
    
    public SndrSyncConnection(ConnectionStateListener listener, final String remoteDeviceUuid, final ClientChannel channel) {
        this.listener = listener;
        this.remoteDeviceUuid = remoteDeviceUuid;
        this.channel = channel;
        this.lastContacted = System.currentTimeMillis();
    }
    //TODO Maybe there needs to be some sort of Thread registering on this connection
    //and throw concurrent exception if multiple threads attempt to use this
    //one connection at the same time.
    
    
    @Override
    public final String getRemoteDeviceUuid() {
        return this.remoteDeviceUuid;
    }

    @Override
    public final ConnectionType getConnectionType() {
        return ConnectionType.SNDR_SYNC;
    }
    
    @Override
    public boolean isConnected() {
        return this.channel.getClientChannel().isConnected();
    }

    @Override
    public void close() {
        setState(State.CLOSING);
        this.channel.close();
        setState(State.CLOSED);
    }

    @Override
    public long getLastContacted() {
        return this.lastContacted;
    }

    @Override
    public final State getState() {
        return this.state;
    }
    
    @Override
    public final void setState(State state) {
        State old = this.state;
        this.state = state;
        if (null != listener) {
            listener.onStateChanged(this, old);
        }
    }
    
    public ClientChannel getChannel() {
        return this.channel;
    }
}