package server;

import fm.icelink.DataBuffer;
import icelink.RemoteConnection;
import icelink.RemoteConnection.Header;
import icelink.RemoteConnection.ReceiveListener;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public final class IceLinkServer implements ReceiveListener {
    
    IceLinkServer() {
        
    }

    private final void createWorker(final RemoteConnection connection, final Header header, final DataBuffer data) {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                boolean success = Processor.INSTANCE.process(connection, header, data);
                System.out.println("IceLink Worker "+(success? "success" : "failure"));
            }
        };
        //logger.log(Level.FINE, "\tStarted worker. "+this.workerExecutor.getTaskCount());
        //this.workerExecutor.execute(task);
        Server.workerExecutor.execute(task);
    }

    @Override
    public void receiveString(final RemoteConnection connection, final String message) {
        System.out.println("Received String "+connection.getConnectionId()+" "+Thread.currentThread().getName());
        System.out.println(message);
    }

    @Override
    public void receiveDataBuffer(final RemoteConnection connection, final DataBuffer data) {
        System.out.println("Received data buffer "+connection.getConnectionId()+" "+Thread.currentThread().getName());
    }

    @Override
    public void receiveProtobuf(final RemoteConnection connection, final Header header, final DataBuffer data) {
        //createWorker(connection, header, data);
    }

    @Override
    public void receiveProtobuf(final RemoteConnection connection, final ByteArrayInputStream bais) {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                boolean success = false;
                try {
                    success = Processor.INSTANCE.process(connection, bais);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("IceLink Worker "+(success? "success" : "failure"));
            }
        };
        //logger.log(Level.FINE, "\tStarted worker. "+this.workerExecutor.getTaskCount());
        //this.workerExecutor.execute(task);
        Server.workerExecutor.execute(task);
    }
}
