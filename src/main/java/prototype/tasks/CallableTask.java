package prototype.tasks;

import java.util.concurrent.Callable;


public final class CallableTask<V> implements Callable<V> {
    Task<V> task;
    CallableTask() {
    }
 
    @Override
    public final V call() throws Exception {
        return task.call();
    }
}