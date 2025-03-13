package common.connection;

import util.Config.ConnectionType;

public interface Connection {
    public String getRemoteDeviceUuid();
    public ConnectionType getConnectionType();
    public boolean isConnected();
    public void close();
    public long getLastContacted();
    public State getState();
    void setState(State state);
    
    public enum State {
        /** The initial connection state.*/
        INITIAL, 
        /** Set when the connection process is started.*/
        CONNECTING, 
        /** Set when the connection is connected and ready to be used.*/
        CONNECTED, 
        /** Set when the connection is in use. Only one thread may use a connection at a time.*/
        IN_USE, 
        /** Set when the connection process failed.*/ 
        FAILED, 
        /** Set when the connection process is closing.*/
        CLOSING, 
        /** Set when the connection is closed. Do not reuse this connection.*/
        CLOSED;
    }
}