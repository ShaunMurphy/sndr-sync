package prototype.tasks;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import prototype.Request;
import util.Config.ConnectionType;
import client.connection.ConnectionManager;

import com.google.protobuf.GeneratedMessageV3;
import common.connection.Connection;
import common.manager.CoreManager;

public final class TaskManager {
    private final int maxRequests = 64;
    private final ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(maxRequests, true);
    private final ExecutorService executor;
    private final Deque<SndrBlockCall<? extends GeneratedMessageV3>> runningCalls = new ArrayDeque<>();
    
    private final ConnectionManager connectionManager;
    
    public TaskManager(ConnectionManager manager) {
        this.connectionManager = manager;
        ThreadFactory factory = CoreManager.createThreadFactory("Task Manager ");
        this.executor = new ThreadPoolExecutor(1, 1, 1, TimeUnit.HOURS, queue, factory);
    }

    public final <T extends GeneratedMessageV3> Call<T> createCall(Request request) {
        return new SndrBlockCall<>(this, request);
    }
    
    public final synchronized void cancelAll() {
        for(SndrBlockCall<?> call : runningCalls) {
            call.cancel();
        }
    }

    /**
     * This is called when the task is enqueued.
     * @param call
     */
    final <T extends GeneratedMessageV3> void enqueue(SndrBlockCall<T> call) {
        synchronized (this) {       
            if(runningCalls.size() < maxRequests) {
                runningCalls.add(call);
                executor.submit(call);
            } else {
                throw new RejectedExecutionException("The current running calls size has exceeded the limit of "+maxRequests+". "
                        + "The newest call will not be executed.");
            }
        }
    }

    /**
     * This is called when the task's "call()" method is called.
     * @param call
     */
    final <T extends GeneratedMessageV3> void called(SndrBlockCall<T> call) {
        if(!runningCalls.contains(call)) {
            runningCalls.add(call);
        }
    }
    
    /**
     * This is called when the task is finished.
     * @param call
     */
    final <T extends GeneratedMessageV3> void finished(SndrBlockCall<T> call) {
        synchronized (runningCalls) {
            if(!runningCalls.remove(call)) {
                throw new AssertionError("The TaskCall called finished but it was never running.");
            }
        }
    }

    //TODO Throw more exceptions.
    public Connection getConnection(ConnectionType type, String deviceUuid) throws IllegalStateException {
        Connection connection = this.connectionManager.getConnection(type, deviceUuid);
        if(connection == null) {
            this.connectionManager.createConnection(type, deviceUuid);
            connection = this.connectionManager.getConnection(type, deviceUuid);
        }
        //If the connection is still null...
        if(connection == null) {
            throw new IllegalStateException("Could not create "+type+" connection for device "+deviceUuid+".");
        }
        return connection;
    }    
}