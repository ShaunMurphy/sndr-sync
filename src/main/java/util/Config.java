package util;

import java.io.File;
import java.nio.file.Paths;


public final class Config {

    private Config() {}

    //Connection
    public static final int CONNECTION_POLL_TIMEOUT = 60000;
    public enum ConnectionType {
        ICELINK, SNDR_SYNC;
    }
    
    /**
     * Returns the duration in milliseconds for how long a connection must be
     * inactive for it to be considered expired.
     * @param type
     * @return
     */
    public static final int INACTIVE_CONNECTION_TIME(ConnectionType type) {
        switch(type) {
        case ICELINK:
            return 300000;
        case SNDR_SYNC:
            return 120000;
        }
        return -1;
    }
    
    //Socket Channels
    //public static boolean CHANGE_SOCKET_BUFFER_SIZE = true;
    
    /** The maximum number of pending connections on a socket. */
    public static final int MAX_SOCKET_CONNECTIONS = 5;

    //Multicast and Unicast
    /**Used to set the default time-to-live for multicast packets sent out on this MulticastSocket 
     * in order to control the scope of the multicasts. The ttl must be in the range 0 <= ttl <= 255.*/
    public static final int MULTICAST_TTL = 5;

    // The the default timeout for connecting to a socket
    public static final int DEFAULT_CONNECT_TIMEOUT = 5000;

    // The the default timeout for receiving a response after sending a multicast packet
    public static final int DEFAULT_MULTICAST_RESPONSE_TIMEOUT = 5000;

    /** The amount of time in seconds to wait for accepting a socket connection. Value = {@value} seconds*/
    public static final int CLIENT_SOCKET_TIMEOUT = 15;

    /** The amount of time in seconds to wait for accepting a socket connection. Value = {@value} seconds*/
    public static final int SERVER_SOCKET_TIMEOUT = 15;

    /** The max number of packets that should be stored during a receive DatagramPacket loop.*/
    public static final int RECEIVE_PACKET_LIMIT = 100;
    
    public static final int RECEIVE_PACKET_TIMEOUT = 3000;
    
    /** When true, disables multicast from receiving packets from itself (when data is sent). */
    public static final boolean DISABLE_MULTICAST_LOOPBACK = false;

    //Buffers
    public static final int DEFAULT_BUFFER_SIZE = 128*1024;
    public static final int OVERRIDE_SOCKET_BUFFER_SIZE = 128*1024;

    
    //SndrBlock
    private static File SERVER_ROOT_DIRECTORY = null;
    private static File CLIENT_ROOT_DIRECTORY = null;
    private static File SERVER_TEMP_DIRECTORY = null;
    private static File CLIENT_TEMP_DIRECTORY = null;

    
    //TODO What should this be doing?
    public static final void setServerRootDirectory(File directory) {
        if(!directory.isDirectory()) {
            throw new IllegalArgumentException("The directory must be a directory.");
        }
        SERVER_ROOT_DIRECTORY = directory;
        if(!SERVER_ROOT_DIRECTORY.exists()) {
            SERVER_ROOT_DIRECTORY.mkdir();
        }
    }
    /**
     * Set root directory for the sndr block.
     * @param directory
     */
    public static final void setRootDirectory(File directory) {
        SERVER_ROOT_DIRECTORY = new File(directory, "SndrBlockFiles_Server");
        CLIENT_ROOT_DIRECTORY = new File(directory, "SndrBlockFiles_Client");
        SERVER_TEMP_DIRECTORY = new File(SERVER_ROOT_DIRECTORY, "temp");
        CLIENT_TEMP_DIRECTORY = new File(CLIENT_ROOT_DIRECTORY, "temp");
        
        if(!SERVER_ROOT_DIRECTORY.exists()) {
            SERVER_ROOT_DIRECTORY.mkdir();
        }
        if(!SERVER_TEMP_DIRECTORY.exists()) {
            SERVER_TEMP_DIRECTORY.mkdir();
        }
    }
    
    
    public static final File SERVER_ROOT_DIRECTORY() {
        return Config.SERVER_ROOT_DIRECTORY;
    }
    
    /**
     * Converts the directory into a Sndr-block path.
     * @param directory
     * @return
     */
    public static final File resolveDirectory(String directory) {
        return Paths.get(Config.SERVER_ROOT_DIRECTORY.getAbsolutePath(), directory).toFile();
    }

    /**
     * Converts the directory and file into a Sndr-block path.
     * @param directory
     * @param file
     * @return
     */
    public static final File resolveFile(String directory, String file) {
        return Paths.get(Config.SERVER_ROOT_DIRECTORY.getAbsolutePath(), directory, file).toFile();
    }
    
}