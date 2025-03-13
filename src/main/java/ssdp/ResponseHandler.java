package ssdp;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;

import ssdp.models.SsdpPacket;

public interface ResponseHandler {
    public void processMessage(DatagramPacket packet);
        /**
     * Adds a client to the client hash map.
     * @param address
     * @param data
     */
    public void addClient(InetSocketAddress address, SsdpPacket.Data data);
}
