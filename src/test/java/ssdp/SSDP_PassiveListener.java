package ssdp;

import com.sndr.logger.SndrLogger;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import ssdp.jobs.MulticastListener;
import ssdp.jobs.PacketReceivedHandler;
import ssdp.message.SsdpConstants;
import ssdp.models.SsdpPacket;
import ssdp.util.NetworkUtil;


public final class SSDP_PassiveListener implements PacketReceivedHandler {

    public static void main(String[] args) throws UnknownHostException {
        System.out.println("Logging all detected SSDP packets for 5 minutes.");
        new SndrLogger.Builder("SSDP_PassiveListener").enableConsoleLogger().build();
        SsdpConstants.initialize(InetAddress.getByName("0:0:0:0:0:0:0:0"), SsdpConstants.DEVICE_TYPES.DESKTOP, UUID.randomUUID().toString());
        new SSDP_PassiveListener();
        try {
            new CountDownLatch(1).await(5, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Logging packets stoppped.");
    }

    private SSDP_PassiveListener() {
        List<NetworkInterface> interfaces = NetworkUtil.getNetworkInterfaces();
        final MulticastListener listener = new MulticastListener(interfaces, this);

        Thread t = new Thread() {
            @Override
            public void run() {
                listener.run();
            }
        };
        t.setDaemon(true);
        t.start();
    }

    @Override
    public void processPacket(DatagramPacket packet) {
        //Parse the data.
        SsdpPacket p = new SsdpPacket(packet);
        System.out.println(p.getData());
    }
}
