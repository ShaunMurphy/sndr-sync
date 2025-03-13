package ssdp;

import com.sndr.logger.SndrLogger;
import ssdp.jobs.MulticastListener;
import ssdp.jobs.ResponderTask;
import ssdp.jobs.UnicastListener;
import ssdp.models.SsdpPacket.Data;
import ssdp.util.NetworkUtil;

import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class SsdpController {
    private final Logger logger = SndrLogger.getLogger();
    private ExecutorService executor;
    //For any service requests, use this.
    private ThreadPoolExecutor serviceExecutor;
    private final ResponderTask responderTask;
    private final MulticastListener multicastListener;
    private UnicastListener unicastListener;
    private final PacketAnalyzer analyzer;
    private final ThreadFactory factory = new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName(SsdpController.class.getSimpleName()+" "+t.getName());
            t.setDaemon(true);
            return t;
        }
    };
    
    private final Config config;

    public SsdpController(final Config config) {
        this.config = (config != null) ? config : new Config();
        if(this.config.interfaces.isEmpty()) {
            this.config.interfaces = NetworkUtil.getNetworkInterfaces();
        }
        this.analyzer = new PacketAnalyzer(this);

        this.responderTask = new ResponderTask();

        this.multicastListener = new MulticastListener(this.config.interfaces, this.analyzer);
        //this.unicastListener = new UnicastListener(host, port, config.interfaces);
    }

    public static final class Config {
        public boolean enableDiscoverSender = false;
        public boolean enablePeriodicSender = false;
        public boolean enableMulticastListener = true;
        public boolean enableUnicastListener = true;
        public List<NetworkInterface> interfaces = new ArrayList<>();
    }
    
    public final void start() {
        if(executor == null || executor.isShutdown()) {
            this.executor = Executors.newFixedThreadPool(5, factory);
        }

        this.executor.execute(responderTask);

        if(config.enableDiscoverSender) {
        }
        if(config.enablePeriodicSender) {
        }
        if(config.enableMulticastListener && multicastListener != null) {
            this.executor.execute(multicastListener);
        }
        if(config.enableUnicastListener && unicastListener != null) {
            this.executor.execute(unicastListener);
        }
    }
    
    public final void stop() {
        if(this.multicastListener != null) {
            this.multicastListener.stop();
        }
        if(this.unicastListener != null) {
            this.unicastListener.stop();
        }
        //Others...
        if(executor != null) {
            executor.shutdown();
            try {
                executor.awaitTermination(5, TimeUnit.SECONDS);
                executor.shutdownNow();
            } catch(InterruptedException e) {
                //Ignore
            }
        }
    }

    public final void sendOKMessage(Data data) {
        this.responderTask.addOKMessage(data.remoteAddress, data.remotePort, data.ST);
    }

    public final PacketAnalyzer getPacketAnalyzer() {
        return this.analyzer;
    }
}