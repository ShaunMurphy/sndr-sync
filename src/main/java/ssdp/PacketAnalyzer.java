package ssdp;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

import ssdp.jobs.PacketReceivedHandler;
import ssdp.message.SsdpConstants;
import ssdp.models.SsdpPacket;



public final class PacketAnalyzer implements PacketReceivedHandler {
    //White list - Search targets who to reply to.
    private Set<String> ST_deviceWhiteList = new HashSet<>();
    private final String createConnectionST;
    private final SsdpController controller;
    private CreateConnectionHandler createConnectionHandler;
    private final boolean logSndrPackets = false;
    
    /*//TODO How to detect this?!?
   If there is an error with the search request (such as an invalid field value in the MAN header
   field, a missing MX header field, or other malformed content), the device shall silently discard
   and ignore the search request; sending of error responses is PROHIBITED due to the
   possibility of packet storms if many devices send an error response to the same request. 
   */
    
    public PacketAnalyzer(SsdpController controller) {
        this.controller = controller;
        //ST_deviceWhiteList.add("upnp:rootdevice");
        createConnectionST = "uuid:"+SsdpConstants.deviceUUID;
        ST_deviceWhiteList.add(createConnectionST);
        String target1 = SsdpConstants.ST_DOMAIN_DEVICE_TYPE_VERSION(SsdpConstants.currentDevice, SsdpConstants.PRODUCT_VERSION);
        target1 = target1.substring(3);
        ST_deviceWhiteList.add(target1);
        
        String target2 = SsdpConstants.ST_DOMAIN_DEVICE_TYPE_VERSION(SsdpConstants.DEVICE_TYPES.ALL, SsdpConstants.PRODUCT_VERSION);
        target2 = target2.substring(3);
        ST_deviceWhiteList.add(target2);
    }
    
    public void setCreateConnectionHandler(CreateConnectionHandler createConnectionHandler) {
        this.createConnectionHandler = createConnectionHandler;
    }
    
    @Override
    public final void processPacket(final DatagramPacket p) {
        //TODO Maybe this needs to go on a queue? Without a queue, the parse is 0 to 1 ms.
        SsdpPacket packet = new SsdpPacket(p);
        //Testing
        /*String a = packet.getAddress().toString();
        if(!(a.endsWith(".121") || a.endsWith(".13") || a.endsWith(".19") || a.endsWith(".33") || a.endsWith(".34") || a.endsWith(".215") || a.endsWith("56.1"))) {
            if(packet.getData().ST != null && !packet.getData().ST.endsWith("service:dial:1")) {
                System.out.printf("%-15s:%-6s %-20s | ST:%s%n", packet.getAddress(), packet.getPort(), packet.getData().SL, packet.getData().ST);
            }
        }*/

        if(packet.isNotify()) {
            processNotify(packet);
        } else if(packet.isMSearch()) {
            processMSearch(packet, packet.getAddress(), packet.getPort());
        } else if(packet.isOK()) {
            processOK(packet);
        } else {
            System.out.println("Unknown packet type! ");
        }
    }
    
    private final void processNotify(final SsdpPacket packet) {
        //TODO
    }
    
    private final void processMSearch(final SsdpPacket packet, InetAddress remoteAddress, int remotePort) {
        SsdpPacket.Data data = packet.getData();
        if(data.checkMalformedRequest()) {
            //Silently ignore the packet. It was malformed.
            return;
        }
        
        if(data.ST == null) {
            return;
        }

        //This is for creating a connection.
        if(data.ST.equals(createConnectionST)) {
            if(this.createConnectionHandler != null) {
                createConnectionHandler.createConnection(data);
            }
        } else {
            //For now, only send this if the packet ST is not this device UUID.
            boolean match = ST_deviceWhiteList.contains(data.ST);
            if(match) {
                //Need to send an OK.
                this.controller.sendOKMessage(data);
            }
        }
        
        if(logSndrPackets) {
            if((data.ST.contains("sndr") || (data.USN != null && data.USN.contains("sndr")))) {               
                System.out.println("Packet Analyzer - "+data);
            } 
        }
        
        
        /*
        //Iterate over the available services and call the service that
        //matches the Search Target
        
        int start = data.ST.lastIndexOf("service:")+8;
        if(start < 8) {
            return;
        }
        int end = data.ST.indexOf(":", start);
        final String type = data.ST.substring(start, end);
        SndrSync.get().processRequest(type, data);//*/
    }

    private final void processOK(final SsdpPacket packet) {
        //TODO
    }
}