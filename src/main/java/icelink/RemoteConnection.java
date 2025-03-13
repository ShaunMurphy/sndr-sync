package icelink;

import java.io.ByteArrayInputStream;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sndr.logger.SndrLogger;
import com.sndr.proto.SndrBlockProto.RequestType;

import fm.icelink.Candidate;
import fm.icelink.Connection;
import fm.icelink.DataBuffer;
import fm.icelink.DataChannel;
import fm.icelink.DataChannelCollection;
import fm.icelink.DataChannelReceiveArgs;
import fm.icelink.DataChannelState;
import fm.icelink.DataChannelWrapper;
import fm.icelink.DataStream;
import fm.icelink.IAction1;
import fm.icelink.IAction2;
import fm.icelink.IceServer;
import fm.icelink.SessionDescription;

/**
 * A class to interact with the IceLink library. Provides methods to create offers, answer, send and receive data.
 * @author shaun
 *
 */
public final class RemoteConnection {
    private static final Logger logger = SndrLogger.getLogger();
    private static final int findClientTimeout = 300000;//ms
    private final boolean debug = false;
    private static final IceServer[] iceServers = new IceServer[] {
        //new IceServer("stun:turn.icelink.fm:3478"),
        //new IceServer("turn:turn.icelink.fm:443?transport=udp", "test", "pa55w0rd!"),
        //new IceServer("turn:turn.icelink.fm:443?transport=tcp", "test", "pa55w0rd!"),
        new IceServer("stun:stun.l.google.com:19302"),
        new IceServer("stun:stun1.l.google.com:19302"),
        new IceServer("stun:stun2.l.google.com:19302"),
        new IceServer("stun:stun3.l.google.com:19302"),
        new IceServer("stun:stun4.l.google.com:19302")
     };
    private static Object channelLock = new Object();
    /** An ID used to identify this connection instance. */
    private long lastContacted;
    private IAction1<Connection> temporaryChange = null;
    static {
        IceLinkUtil.loadLicense_hardcoded();
    }

    private final DataChannelCollection dataChannels;
    private final DataChannel protobufChannel, fileChannel;
    private final Connection connection;
    private ConnectionStateListener connectionStateListener;
    
    public RemoteConnection() {
        UUID connectionId = UUID.randomUUID();
        this.dataChannels = new DataChannelCollection();
        this.protobufChannel = createDataChannel("protobuf-channel", dataChannels);
        this.fileChannel = createDataChannel("file-channel", dataChannels);
        DataStream protobufStream = new DataStream(protobufChannel);
        DataStream fileStream = new DataStream(fileChannel);
        //stream.setEncryptionModes(new EncryptionMode[]{EncryptionMode.Null});//Doesn't work
        this.connection = createConnection(connectionId.toString(), protobufStream, fileStream);
    }

    public final void close() {
        if(connection != null) {
            connection.close();
        }
    }

    public interface ConnectionStateListener {
        public void connectionStateChanged(ConnectionState state);
    }
    
    public interface ConnectionListener {
        public void offerCreated(SessionDescription offer);
        public void answerCreated(SessionDescription answer);
    }
    
    public interface ReceiveListener {
        /**
         * This is called when ever a string is received.
         * @param connection
         * @param message
         */
        public void receiveString(RemoteConnection connection, String message);
        /**
         * This is called during the data buffer received callback.
         * @param connection
         * @param data
         */
        public void receiveDataBuffer(RemoteConnection connection, DataBuffer data);
        /**
         * This is called during the protobuf received callback.
         * @param connection
         * @param header
         * @param data
         */
        public void receiveProtobuf(RemoteConnection connection, Header header, DataBuffer data);
        
        public void receiveProtobuf(RemoteConnection connection, ByteArrayInputStream bais);
    }

    /**
     * A simple header used to send protobuf messages.
     */
    public static final class Header {
        public final int size;
        public final boolean isRequest;
        public final RequestType type;
        /** The size of this header in bytes.*/
        private final static int headerSize = 12;
        private Header(DataBuffer buffer) {
            int enumIndex = (int) buffer.read32(0);
            this.size = (int) buffer.read32(4);
            this.isRequest = buffer.read32(8) == 0;
            this.type = RequestType.forNumber(enumIndex);
        }
    }
    
    /**
     * Creates a connection with the given streams.
     * @param id
     * @param streams
     * @return
     */
    private final Connection createConnection(final String id, final DataStream... streams) {
        final Connection connection = new Connection(streams);
        connection.setId(id);
        connection.setTimeout(findClientTimeout);
        connection.setIceServers(iceServers);
        connection.addOnStateChange(new IAction1<Connection>() {
            @Override
            public void invoke(Connection connection) {
                String id = connection.getId();
                if(debug) {
                    logger.log(Level.INFO, id+" Connection state: "+connection.getState().name());
                }
                if(connectionStateListener != null) {
                    connectionStateListener.connectionStateChanged(getConnectionState());
                }
            }
        });
        if(debug) {
            connection.addOnLocalCandidate(new IAction2<Connection, Candidate>() {
                @Override
                public void invoke(Connection connection, Candidate candidate) {
                    String id = connection.getId();
                    logger.log(Level.INFO, id+" Local Candidate: "+candidate.toJson()); 
                }
            });
            connection.addOnRemoteCandidate(new IAction2<Connection, Candidate>() {
                @Override
                public void invoke(Connection connection, Candidate candidate) {
                    String id = connection.getId();
                    logger.log(Level.INFO, id+" Remote Candidate: "+candidate.toJson());                
                }
            });
            connection.addOnSignallingStateChange(new IAction1<Connection>() {
                @Override
                public void invoke(Connection connection) {
                    String id = connection.getId();
                    logger.log(Level.INFO, id+" Connection signalling state change: "+connection.getState().name());                                
                }
            });
        }
        return connection;
    }

    /**
     * Creates a data channel. This is needed when creating a {@link DataStream}.
     * @param label - The data channel name. Can be used for identifying a data channel.
     * @param dataChannels - Adds this channel to this collection when connected, 
     * removes this channel from this collection when closed or failed.
     * @return
     */
    private final DataChannel createDataChannel(final String label, final DataChannelCollection dataChannels) {
        final DataChannel channel = new DataChannelWrapper(label);
        channel.addOnStateChange(new IAction1<DataChannel>() {
            @Override
            public void invoke(DataChannel channel) {
                if(debug) {
                    logger.log(Level.INFO, "DataChannel "+channel.getLabel()+" state changed to "+channel.getState());
                }
                if(channel.getState() == DataChannelState.Connected) {
                    synchronized (channelLock) {
                        //System.out.println("Added "+channel.getLabel());
                        dataChannels.add(channel);
                    }
                } else if(channel.getState() == DataChannelState.Closed
                        || channel.getState() == DataChannelState.Failed) {
                    synchronized (channelLock) {
                        //System.out.println("Removed "+channel.getLabel());
                        dataChannels.remove(channel);
                    }
                }
            }
        });
        return channel;
    }

    /**
     * Sets the listener to the channel to receive updates when a message is received.
     * @param listener - Used to receive messages when a message is received by the data channel.
     */    
    public final void setProtobufListener(final ReceiveListener listener) {
        this.protobufChannel.setOnReceive(new IAction1<DataChannelReceiveArgs>() {
            @Override
            public void invoke(DataChannelReceiveArgs data) {
                updateLastContacted();
                if(data.getDataString() != null) {
                    listener.receiveString(RemoteConnection.this, data.getDataString());
                } else if(data.getDataBytes() != null) {
                    DataBuffer buffer = data.getDataBytes();
                    //Parse the header.
                    //Header header = new Header(buffer);
                    //Remove the header from the DataBuffer.
                    //byte[] bytes = new byte[buffer.getLength() - Header.headerSize];
                    //System.arraycopy(buffer.getData(), Header.headerSize, bytes, 0, bytes.length);
                    //buffer = DataBuffer.wrap(bytes);
                    //listener.receiveProtobuf(RemoteConnection.this, header, buffer);
                
                    
                    ByteArrayInputStream bais = new ByteArrayInputStream(buffer.getData());

                    listener.receiveProtobuf(RemoteConnection.this, bais);
                }
            }
        });        
    }

    /**
     * Sets the listener to the channel to receive updates when a message is received.
     * @param listener - Used to receive messages when a message is received by the data channel.
     */
    public final void setFileListener(final ReceiveListener listener) {
        this.fileChannel.setOnReceive(new IAction1<DataChannelReceiveArgs>() {
            @Override
            public void invoke(DataChannelReceiveArgs data) {
                if(data.getDataString() != null) {
                    listener.receiveString(RemoteConnection.this, data.getDataString());
                } else if(data.getDataBytes() != null) {
                    listener.receiveDataBuffer(RemoteConnection.this, data.getDataBytes());
                }
            }
        });
    }

    /** The current connection state.*/
    public enum ConnectionState {
        New, Initializing, 
        Connecting, Connected,
        Failing, Failed,
        Closing, Closed;
        
        public static ConnectionState get(fm.icelink.ConnectionState state) {
            return ConnectionState.valueOf(state.name());
        }
    }
    
    public final ConnectionState getConnectionState() {
        return ConnectionState.get(this.connection.getState());
    }
    
    private void updateLastContacted() {
        this.lastContacted = System.currentTimeMillis();
    }
    
    /**
     * Returns the system time that this connection last sent or received a protobuf object.
     * @return
     */
    public final long getLastContacted() {
        return this.lastContacted;
    }
    
    //TODO The methods (create offer/answer) should set a state and prevent the other from being called.
    /**
     * Asynchronously creates an offer. The listener.offerCreated() method will be called when complete.
     * @param listener
     */
    public final void createOffer(final ConnectionListener listener) {
        this.connection.createOffer().then(new IAction1<SessionDescription>() {
            @Override
            public void invoke(SessionDescription offer) {
                listener.offerCreated(offer);
                connection.setLocalDescription(offer);
            }
        });
    }

    /**
     * Asynchronously creates an answer. The listener.answerCreated() method will be called when complete.
     * @param listener
     */    
    public final void createAnswer(final ConnectionListener listener) {
        //Make sure the set offer json is called before this method.
        if(this.connection.getRemoteDescription() == null) {
            throw new IllegalStateException("The remote description offer must be set before calling this method.");
        }

        /*
        this.connection.createAnswer().then(new IAction1<SessionDescription>() {
            @Override
            public void invoke(SessionDescription answer) {
                //listener.answerCreated(answer);
                //connection.setLocalDescription(answer);
            }
        });
        //*/
        
        
        //Ignore this result.
        this.connection.createAnswer().waitForResult();
        this.temporaryChange = new IAction1<Connection>() {
            @Override
            public void invoke(Connection c) {
                SessionDescription answer = connection.createAnswer().waitForResult();
                if(debug) {
                    logger.log(Level.INFO, "========== Answer =================");
                    logger.log(Level.INFO, answer.getSdpMessage().toString());
                }
                listener.answerCreated(answer);
                connection.setLocalDescription(answer);
                connection.removeOnGatheringStateChange(temporaryChange);
            }
        };
        this.connection.addOnGatheringStateChange(this.temporaryChange);
    }

    public final void setOfferJson(String offerJson) {
        SessionDescription offer = SessionDescription.fromJson(offerJson);
        if (offer.getSdpMessage() == null) {
            logger.log(Level.SEVERE, "Failed to read offer json. Json:\n"+offerJson);
            return;
        } else {
            if(debug) {
                logger.log(Level.INFO, "========== Offer =================");
                logger.log(Level.INFO, offer.getSdpMessage().toString());
            }
        }
        this.connection.setRemoteDescription(offer);
    }
    
    public final void setAnswerJson(String answerJson) {
        SessionDescription answer = SessionDescription.fromJson(answerJson);
        if (answer.getSdpMessage() == null) {
            logger.log(Level.SEVERE, "Failed to read answer json.\nJson:"+answerJson);
            return;
        } else {
            if(debug) {
                logger.log(Level.INFO, "========== Answer =================");
                logger.log(Level.INFO, answer.getSdpMessage().toString());
            }
        }
        this.connection.setRemoteDescription(answer);
    }

    /**
     * Returns the ID used to identify this connection instance.
     * @return
     */
    public final String getConnectionId() {
        return this.connection.getId();
    }

    /**
     * Returns the DataChannel that should be used for sending and receiving files.
     * @return
     */
    public final DataChannel getFileChannel() {
        return this.fileChannel;
    }

    /**
     * Returns the DataChannel that should be used for sending and receiving protobufs.
     * @return
     */
    public final DataChannel getProtobufChannel() {
        return this.protobufChannel;
    }

    public final void setConnectionId(String id) {
        this.connection.setId(id);
    }

    public void setConnectionStateListener(ConnectionStateListener listener) {
        this.connectionStateListener = listener;
    }
}