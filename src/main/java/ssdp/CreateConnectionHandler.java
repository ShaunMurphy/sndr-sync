package ssdp;

import ssdp.models.SsdpPacket;

public interface CreateConnectionHandler {
    void createConnection(SsdpPacket.Data data);
}