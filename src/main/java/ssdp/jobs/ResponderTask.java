package ssdp.jobs;

import java.net.InetAddress;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import ssdp.message.MessageGenerator;

import com.sndr.logger.SndrLogger;
import java.io.IOException;
import java.net.SocketException;
import ssdp.message.SsdpUnicastSocket;

public final class ResponderTask implements Runnable {
    private static final Logger LOGGER = SndrLogger.getLogger();

    private final LinkedBlockingQueue<QueueData> queue = new LinkedBlockingQueue<>(100);
    private boolean keepCheckingQueue = true;
    private final Random random = new Random();

    public void addOKMessage(InetAddress remoteAddress, int remotePort, String searchTarget) {
        String message = MessageGenerator.mSearchOK(searchTarget, "localhost");
        QueueData data = new QueueData(remoteAddress, remotePort, message);
        try {
            queue.put(data);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void run() {
        while(keepCheckingQueue) {
            try {
                //Wait for the next message on the queue.
                //This will block the thread.
                QueueData data = queue.take();
                //A send delay.
                Thread.sleep(random.nextInt(250));
                try {
                    //Send the message.
                    sendOKMessage(data.remoteAddress, data.remotePort, data.message);
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, "Failed to send OK message", ex);
                }
            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, "The queue thread was interrupted.", e);
            }
        }
    }
    
    private void sendOKMessage(InetAddress remoteAddress, int remotePort, String message) throws SocketException, IOException {
        //System.out.println("Sent OK message to "+remoteAddress.getHostAddress()+":"+remotePort);
        //System.out.println(message+"\n");        
        SsdpUnicastSocket socket = new SsdpUnicastSocket(remoteAddress, remotePort);
        socket.send(message, 0);
        socket.close();
    }
 
    private final class QueueData {
        private final InetAddress remoteAddress;
        private final int remotePort;
        private final String message;
        private QueueData(InetAddress remoteAddress, int remotePort, String message) {
            this.remoteAddress = remoteAddress;
            this.remotePort = remotePort;
            this.message = message;
        }
    }
}