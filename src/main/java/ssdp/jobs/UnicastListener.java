package ssdp.jobs;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.util.List;

import ssdp.message.SsdpMulticastSocket;


/**
 * Listens for all Unicast packets on the specified host address and port.
 * @author shaun
 */

//NEEDS TESTING!
public class UnicastListener implements Runnable {
    private SsdpMulticastSocket socket = null;
    private boolean keepRunning = true;
    public UnicastListener(InetAddress host, int port, List<NetworkInterface> networkInterfaces) {
        
    }

    @Override
    public void run() {
        Thread.currentThread().setName("UnicastListener");
        //https://bugs.openjdk.java.net/browse/JDK-8072466
        //This allows the unicast listener to be created first.
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //this.socket = super.createSocket();
        socket.setBlocking(true);
        while (!Thread.interrupted() && keepRunning) {
            DatagramPacket packet = null;
            try {
                //System.out.println("UnicastListener - Waiting to receive unicast packet. SocketAddress:"+socket.getSocketAddress());
                System.out.println("Socket Info - " + socket);
                packet = socket.receive();
            } catch (SocketTimeoutException e) {
                System.out.println("Unicast listener socket timeout");
                continue;
            } catch (IOException ex) {
                System.out.println("Unicast listener IO exception");
                continue;
            }

            if (packet.getLength() == 0) {// TODO Is this needed?
                System.out.println("packet length == 0");
                continue;
            }
            System.out.println("UnicastListener received packet "+packet.getAddress()+" "+packet.getPort());
            //responseHandler.processMessage(packet);
        }
        // This will close the socket and leave the network group.
        socket.close();
    }
    
    /** Kills this thread by setting the state to stopped. */
    public final void stop() {
        keepRunning = false;
        if(this.socket != null) {
            this.socket.close();
        }
    }
}