package application;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

import ssdp.CreateConnectionHandler;
import ssdp.SsdpController;
import ssdp.message.SsdpConstants;
import application.interfaces.SyncMediator;

import com.sndr.logger.SndrLogger;

public final class SSDP_Main {
    private final SyncMediator syncMediator;
    private final Logger logger = SndrLogger.getLogger();
    private SsdpController controller;
    //For now
    private static SSDP_Main instance;
    public SSDP_Main(SsdpConstants.DEVICE_TYPES type, String deviceUUID, SyncMediator syncMediator) {
        if (null == syncMediator) {
            throw new IllegalArgumentException("invalid params");
        }

        this.syncMediator = syncMediator;
        try {
            //InetAddress localAddress = InetAddress.getByName("0.0.0.0");//Forces IPv4
            InetAddress localAddress = InetAddress.getByName("0:0:0:0:0:0:0:0");//Forces IPv6
            //InetAddress localAddress = InetAddress.getByName("fe80::1156:fdf1:eaff:5032");//Forces IPv6
            SsdpConstants.initialize(localAddress, type, deviceUUID);
        } catch (UnknownHostException e) {
            logger.log(Level.SEVERE, "", e);
        }
        instance = this;
    }
    
    public final void start() {
        //TODO Create the services?
        this.controller = new SsdpController(null);
        this.controller.start();
    }
    
    public final void stop() {
        this.controller.stop();
    }
    
    //Temporary
    public final void setCreateConnectionHandler(CreateConnectionHandler handler) {
        this.controller.getPacketAnalyzer().setCreateConnectionHandler(handler);
    }
    
    //This is here temporarily.
    //This is the reply to the connection request.
//    public final void sendConnectionRequest(final InetSocketAddress serverAddress, final InetSocketAddress clientAddress) {
//        //For now...
//        String ST = SsdpConstants.ST_DEVICE();
//        String location = serverAddress.getHostString()+":"+serverAddress.getPort();
//        String message = MessageGenerator.mSearchOK(ST, location);
//        //System.out.println(message);
//        SsdpSocket socket = new SsdpSocket(SsdpSocket.Type.Unicast, clientAddress.getAddress(), clientAddress.getPort());
//        socket.sendUnicastMessage(message, 0);
//    }

    public static String getControlPointName() {
        
        //String sha1User = instance.crypto.hashStringToSHABase64(instance.syncMediator.getUser());
        //return sha1User;
        return "no name...";
    } 
}