package common.connection;

/**
 * Used to listen to state changes on a connection
 */
public interface ConnectionStateListener {
    void onStateChanged(Connection connection, Connection.State oldState);
}
