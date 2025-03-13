package prototype.tasks;

import java.io.IOException;

import prototype.Request;
import prototype.Response;

public interface Call<T> extends Cloneable {
    void cancel();
    Call<T> clone();
    void enqueue(Callback<T> callback);
    Response<T> execute() throws IOException;
    boolean isCancelled();
    boolean isExecuted();
    Request request();
}
