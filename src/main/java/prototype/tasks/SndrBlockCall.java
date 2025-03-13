package prototype.tasks;

import icelink.IceLinkInputStream;
import icelink.RemoteConnection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import prototype.Request;
import prototype.Response;

import com.google.protobuf.GeneratedMessageV3;
import com.sndr.logger.SndrLogger;
import common.ClientChannel;
import common.Serializer;
import common.connection.Connection;
import common.connection.IceLinkConnection;
import common.connection.SndrSyncConnection;

/**
 * A task class used to make Client requests to the sndr-block.
 * @author shaun
 *
 * @param <T>
 */
public final class SndrBlockCall<T extends GeneratedMessageV3> extends Task<T> implements Call<T> {

    private static final Logger logger = SndrLogger.getLogger();

    private final Request request;
    private final TaskManager manager;
    private boolean executed = false;
    private Throwable failure;//Used to store any failures.
    private Callback<T> callback = null;
    private Response<T> response = new Response<>();
    
    SndrBlockCall(TaskManager manager, final Request request) {
        this.manager = manager;
        this.request = request;
    }

    @Override
    public final Request request() {
        return this.request;
    }
    
    @Override
    public final void enqueue(Callback<T> callback) {
        synchronized (this) {
            if(executed) {
                throw new IllegalStateException("This call was already executed.");
            }
            this.executed = true;
        }
        if(failure != null) {
            callback.onFailure(this, failure);
            return;
        }
        this.callback = callback;
        try {
            this.manager.enqueue(this);
        } catch(Exception e) {
            failure = e;
            callback.onFailure(this, failure);
        }
    }
    
    @Override
    public Response<T> execute() throws IOException {
        synchronized (this) {
            if(executed) {
                throw new IllegalStateException("This call was already executed.");
            }
            this.executed = true;
        }
        if(failure != null) {
            if(failure instanceof IOException) {
                throw (IOException) failure;
            } else {
                throw (RuntimeException) failure;
            }
        }
        this.call();
        if(failure != null) {
            if(failure instanceof IOException) {
                throw (IOException) failure;
            } else {
                throw (RuntimeException) failure;
            }
        }
        return this.response;
    }    

    @SuppressWarnings("resource")
    @Override
    public final T call() {
        this.manager.called(this);
        Connection connection = null;
        T responseMessage = null;
        try {
            connection = this.manager.getConnection(request.connectionType, request.deviceUuid);
        } catch(IllegalStateException e) {
            this.failure = e;
        }

        if(connection == null) {
            callFailCallback();
            return null;
        }

        if(connection instanceof SndrSyncConnection) {
            //For now...type cast the connection.
            ClientChannel channel = ((SndrSyncConnection)connection).getChannel();
            try {
                OutputStream output = channel.getOutputStream();
                Serializer.INSTANCE.writeDelimitedRequest(request.type, request.messageBuilder, output);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to write request", e);
                this.failure = e;
                connection.close();
                callFailCallback();
                return null;
            }

            //If there is a file. 
            //TODO This needs to be changed.
            if(request.file != null) {
                logger.log(Level.WARNING, "The file transfer is not implemented yet!");
                //Maybe use this? Serializer.INSTANCE.sendFile(path, file, channel);
                //System.out.println("---- start transferring file "+request.file.getName()+" -----");
                //FileTransfer.directCopy(request.file.toPath(), channel.getClientChannel(), request.file.length());
                //System.out.println("---- finished transferring file "+request.file.getName()+" -----");
            }

            //Get response.
            InputStream input;
            try {
                input = channel.getInputStream();
                //TODO Add a timeout here...
                responseMessage = Serializer.INSTANCE.<T>parseDelimitedResponse(request.type, input);
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "Failed to read response", ex);
                this.failure = ex;
                callFailCallback();
                return null;
            }

            this.response.setIsSuccessful(null != responseMessage);
            this.response.setType(request.type);
            this.response.setData(responseMessage);        
        } else if(connection instanceof IceLinkConnection) {
            RemoteConnection remoteConnection = ((IceLinkConnection)connection).getRemoteConnection();
            InputStream input = new IceLinkInputStream(remoteConnection.getProtobufChannel());

            Serializer.INSTANCE.writeDelimitedRequest(request.type, request.messageBuilder, remoteConnection);
            try {
                //Get response
                responseMessage = Serializer.INSTANCE.<T>parseDelimitedResponse(request.type, input);
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "Failed to read response", ex);
                this.failure = ex;
                callFailCallback();
                return null;
            } finally {
                try {
                    input.close();
                } catch (IOException e) {}
            }

            this.response.setIsSuccessful(true);
            this.response.setType(request.type);
            this.response.setData(responseMessage);
        }

        if (callback != null) {
            callback.onResponse(this, this.response);
        }

        this.manager.finished(this);
        return responseMessage;
    }

    private final void callFailCallback() {
        this.manager.finished(this);
        if(callback != null) {
            callback.onFailure(this, failure);
        }
    }

    @Override
    public final Call<T> clone() {
        return new SndrBlockCall<>(this.manager, this.request);
    }

    @Override
    public void cancel() {
        super.cancel(true);
        this.manager.finished(this);
    }

    @Override
    public boolean isExecuted() {
        return this.executed;
    }

    @Override
    public boolean isCancelled() {
        return super.isCancelled();
    }
}