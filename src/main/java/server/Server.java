package server;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sndr.logger.SndrLogger;

import common.ClientChannel;

public final class Server {
    //private static final Logger logger = Logger.getLogger(Server.class.getName());
    private static final Logger logger = SndrLogger.getLogger();
    private final Selector selector;
    private final Object wakeupLock = new Object();
    private boolean keepRunning = true;
    private boolean debug = false;

    private final ThreadPoolExecutor selectorExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.SECONDS, 
            new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("SndrBlock: Server");
            t.setPriority(Thread.MAX_PRIORITY);
            t.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    logger.log(Level.SEVERE, "Uncaught", e);
                }
            });
            return t;
        }
    });
    static final ThreadPoolExecutor workerExecutor = new ThreadPoolExecutor(2, 2, 5L, TimeUnit.SECONDS, 
            new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
            private final AtomicInteger index = new AtomicInteger(1);
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("SndrBlock: Server Worker #"+index.getAndIncrement());
            t.setPriority(Thread.MAX_PRIORITY);
            t.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    logger.log(Level.SEVERE, "Uncaught", e);
                }
            });
            return t;
        }
    });

    Server() {
        Selector selector = null;
        try {
            selector = Selector.open();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.selector = selector;
        selectorExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    startSelector();
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Server selector died.", e);
                }
            }
        });
        //startMonitorThread();
    }

    /**
     * Periodically prints server state information.
     */
    @SuppressWarnings("unused")
    private final void startMonitorThread() {
        new Thread() {
            @Override
            public void run() {
                final int sleep = 15;//seconds
                for(int i=0; i<3600/sleep; i++) {
                    int active = workerExecutor.getActiveCount();
                    int queued = workerExecutor.getQueue().size();
                    long complete = workerExecutor.getCompletedTaskCount();
                    int keys = selector.keys().size();
                    StringBuilder b = new StringBuilder("Server State  ");
                    b.append("|Active:").append(active);
                    b.append("\t|Queued:").append(queued);
                    b.append("\t|Complete:").append(complete);
                    b.append("\t|Selector Keys:").append(keys);
                    logger.log(Level.INFO, b.toString());
                    try {
                        Thread.sleep(sleep*1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    private final void startSelector() throws IOException {
        logger.log(Level.INFO, "SndrBlock Server started");
        while (keepRunning) {
            synchronized (wakeupLock) {
            }
            int ready = selector.select();
            //System.out.println(ready == 0 ? "Not ready" : "ready "+ready);
            if (ready == 0) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }

            
            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = keys.iterator();
            while(iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();
                if(debug) {
                    boolean acceptable = key.isAcceptable();
                    boolean connectable = key.isConnectable();
                    boolean readable = key.isReadable();
                    boolean writable = key.isWritable();
                    logger.log(Level.FINE, key.attachment()+" "+acceptable+" "+connectable+" "+readable+" "+writable);
                }
                
                if(key.isConnectable()) {
                    
                } else if(key.isAcceptable()) {
                    
                } else if(key.isReadable()) {
                    createWorker(key, (SocketChannel) key.channel(), (ClientChannel) key.attachment());
                } else if(key.isWritable()) {
                    
                }
            }
        }
    }

    public final void registerSocketChannel(final ClientChannel clientChannel) {
        //if(registeredChannels.contains(channel)) {
        //    System.out.println("Already contains channel "+channel);
        //    return;
        //}
        @SuppressWarnings("resource")
        SocketChannel channel = clientChannel.getClientChannel();
        try {
            selector.wakeup();
            synchronized (wakeupLock) {
                channel.configureBlocking(false);
                try {
                    channel.register(selector, SelectionKey.OP_READ, clientChannel);
                } catch(CancelledKeyException e) {
                    logger.log(Level.SEVERE, "Couldn't re-register channel, trying again.", e);
                    selector.wakeup();
                    channel.register(selector, SelectionKey.OP_READ, clientChannel);
                }
                //registeredChannels.add(channel);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //TODO?
    public final void unregisterSocketChannel(final ClientChannel clientChannel) {
        //SocketChannel channel = clientChannel.getClientChannel();
        throw new NoSuchMethodError("unregisterSocketChannel is not implemented yet.");
    }

    private final void createWorker(final SelectionKey key, final SocketChannel channel, final ClientChannel clientChannel) {
        key.cancel();
        //registeredChannels.remove(channel);
        Runnable task = new Runnable() {
            @Override
            public void run() {
                //System.out.println("\tworker: "+Thread.currentThread().getName());
                //The channel should be unregistered due to the cancel at this point...
                //Which means you can now block!
                try {
                    channel.configureBlocking(true);
                    boolean success = Processor.INSTANCE.process(clientChannel);
                    if(success) {
                        //Re-register the channel.
                        registerSocketChannel(clientChannel);
                    } else {
                        logger.log(Level.SEVERE, "Did not re-register client channel after processing!");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        //logger.log(Level.FINE, "\tStarted worker. "+this.workerExecutor.getTaskCount());
        workerExecutor.execute(task);
    }

    /**
     * This will shutdown this server. Once shutdown, it cannot be restarted.
     */
    public void shutdown() {
        if(workerExecutor.isShutdown()) {
            return;
        }
        try {
            this.keepRunning = false;
            workerExecutor.shutdown();
            workerExecutor.awaitTermination(5, TimeUnit.SECONDS);
            workerExecutor.shutdownNow();

            selector.close();
            selectorExecutor.shutdown();
            selectorExecutor.awaitTermination(2, TimeUnit.SECONDS);
            selectorExecutor.shutdownNow();
        } catch (InterruptedException | IOException e) {
            logger.log(Level.WARNING, "", e);
        }
    }
}