package common.manager;

import com.sndr.logger.SndrLogger;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public final class CoreManager {
    private static final int DEFAULT_CORE_POOL_SIZE = 10;
    private static final int DEFAULT_MAX_POOL_SIZE = 10;
    private static final int DEFAULT_QUEUE_SIZE = 20;
    private static final String GLOBAL_THREAD_PREFIX = "SndrBlock: ";
    private static final String THREAD_PREFIX = GLOBAL_THREAD_PREFIX+"thread-";
    /**
     * The same Thread Factory as the one in Executors, but sets the thread to daemon.
     * @author shaun
     */
    private static class DefaultThreadFactory implements ThreadFactory {
        //private static final AtomicInteger pool = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger thread = new AtomicInteger(1);
        private final String prefix;
        //private final String namePrefix;
        DefaultThreadFactory(String prefix) {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
            //namePrefix =  threadPrefix+pool.getAndIncrement()+"-thread-";
            this.prefix = prefix;
        }
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r, prefix + thread.getAndIncrement());
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY);
            t.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    SndrLogger.getLogger().log(Level.SEVERE, "Uncaught", e);
                }
            });
            return t;
        }
    }

    protected ThreadPoolExecutor executor;
    private PriorityBlockingQueue<Runnable> queue;
    private static CoreManager instance;

    public static final CoreManager get() {
        if(instance == null) {
            instance = new CoreManager();
        }
        return instance;
    }

    protected CoreManager() {
        this(DEFAULT_CORE_POOL_SIZE, DEFAULT_MAX_POOL_SIZE, DEFAULT_QUEUE_SIZE);      
    }

    protected CoreManager(int corePoolSize, int maxPoolSize, int queueSize) {
        //this.queue = new PriorityBlockingQueue<Runnable>(queueSize, new QueueComparator());
        this.queue = new PriorityBlockingQueue<Runnable>(queueSize, null);
        this.executor = new ThreadPoolExecutor(corePoolSize, maxPoolSize, 1L, TimeUnit.SECONDS, queue, new DefaultThreadFactory(THREAD_PREFIX));
    }

    /*
    private class QueueComparator implements Comparator<Runnable> {
        @Override
        public int compare(Runnable o1, Runnable o2) {
            BaseTask<?> task1 = (BaseTask<?>) o1;
            BaseTask<?> task2 = (BaseTask<?>) o2;
            if (task1.getThreadPriority() < task2.getThreadPriority()) {
                return 1;
            } else if (task1.getThreadPriority() > task2.getThreadPriority()) {
                return -1;
            }
            return 0;
        }
    }
    
    public static final <V> Future<V> execute(final BaseTask<V> task) {
        //Using a submit will cause the comparator to stop working.
        //Do not use executor.submit(task);
        if (task != null) {
            get().executor.execute(task);
        }
        return task;
    }*/

    public static final void execute(Runnable task) {
        if (task != null) {
            get().executor.execute(task);
        }
    }

    /**
     * Returns true if the calling thread belongs to the thread pool in this class.
     * Use this call the ensure that a given task is being executed
     * (or not being executed) in the thread pool.
     *
     * @return true if running in the thread pool.
     */
    public static final boolean isInThreadPool() {
        return Thread.currentThread().getName().startsWith(THREAD_PREFIX);
    }

    public final void shutdown() {
        if(this.executor.isShutdown()) {
            return;
        }
        if(this.queue.isEmpty()) {
            this.executor.shutdown();
            this.executor.shutdownNow();
        } else {
            this.executor.shutdown();
            try {
                this.executor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            this.executor.shutdownNow();
        }
    }

    //----------------------------------------------------------------------------------------

    public static final ThreadFactory createThreadFactory(String prefix) {
        return new DefaultThreadFactory(GLOBAL_THREAD_PREFIX+prefix);
    }
}