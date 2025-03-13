package ssdp.jobs;

import com.sndr.logger.SndrLogger;
import ssdp.message.SsdpConstants;
import util.Config;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

//Listen for all multicast packets on all available network interfaces.
public final class MulticastListener implements Runnable {

    private static final Logger LOGGER = SndrLogger.getLogger();

    private final List<NetworkInterface> networkInterfaces;
    private final PacketReceivedHandler handler;
    private ByteBuffer buffer = ByteBuffer.allocate(1024);

    private boolean keepRunning = true;
    public MulticastListener(List<NetworkInterface> networkInterfaces, PacketReceivedHandler handler) {
        this.networkInterfaces = networkInterfaces;
        this.handler = handler;
    }

    @Override
    public void run() {
        Thread.currentThread().setName("MulticastListener");
        LOGGER.log(Level.INFO, "Starting Multicast listener");

        try (final MulticastSocket multicastSocket = new MulticastSocket(null);) {
            multicastSocket.bind(new InetSocketAddress(1900));
            multicastSocket.setTimeToLive(Config.MULTICAST_TTL);
            //When true, ignores packets from the same ip address.
            multicastSocket.setLoopbackMode(Config.DISABLE_MULTICAST_LOOPBACK);

            //multicastSocket.setSoTimeout(Config.RECEIVE_PACKET_TIMEOUT);
            //System.out.println(multicastSocket.getLocalSocketAddress());
            
            //Prepare the socket for all interfaces.
            for(NetworkInterface i : networkInterfaces) {
                try {
                    if (SsdpConstants.deviceOS.equals(SsdpConstants.OS.Mac)) {
                        multicastSocket.setNetworkInterface(i);
                    }
                    multicastSocket.joinGroup(SsdpConstants.multicastIPv4Socket, i);
                    multicastSocket.joinGroup(SsdpConstants.multicastIPv6Socket, i);
                } catch (SocketException e) {
                    LOGGER.log(Level.WARNING, "Couldn''t join multicast group {0}", i);
                }
            }

            while (!Thread.interrupted() && keepRunning) {    
                DatagramPacket packet = null;
                buffer.clear();
                
                try {
                    packet = new DatagramPacket(buffer.array(), buffer.capacity());
                    multicastSocket.receive(packet);
                } catch (SocketTimeoutException e) {
                    continue;
                }
                if(packet.getLength() == 0) {
                    LOGGER.log(Level.WARNING, "The received packet was empty.");
                    continue;
                }
                //TODO pass packet to the packet analyzer.
                //System.out.println("MulticastListener - received packet from "+packet.getSocketAddress());
                this.handler.processPacket(packet);
            }
            LOGGER.log(Level.INFO, "Closing multicast socket");
            for(NetworkInterface i : networkInterfaces) {
                multicastSocket.leaveGroup(SsdpConstants.multicastIPv4Socket, i);
                multicastSocket.leaveGroup(SsdpConstants.multicastIPv6Socket, i);
            }
            multicastSocket.close();
        } catch(IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to create multicast socket", e);
        }
    }
    
    public final void stop() {
        LOGGER.log(Level.INFO, "Stopping Multicast listener");
        this.keepRunning = false;
    }
}