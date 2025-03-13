package ssdp.jobs;

import java.net.DatagramPacket;

public interface PacketReceivedHandler {
    /**
     * This is called when a datagram packet is received.
     * @param packet
     */
    void processPacket(DatagramPacket packet);
}
