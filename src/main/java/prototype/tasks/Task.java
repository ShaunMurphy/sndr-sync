package prototype.tasks;

import java.util.concurrent.FutureTask;

public abstract class Task<V> extends FutureTask<V> {
    public Task() {
        this(new CallableTask<V>());
    }
    private Task(CallableTask<V> callable) {
        super(callable);
        callable.task = this;
    }
    
    abstract V call();
}