package prototype;

import com.sndr.proto.SndrBlockProto.RequestType;

public class Response<T> {
    private RequestType type;
    private T data;
    private boolean isSuccessful = false;
    
    public T getData() {
        return data;
    }
    
    public boolean isSuccessful() {
        return isSuccessful;
    }

    public void setType(RequestType type) {
        this.type = type;
    }

    public RequestType getRequestType() {
        return this.type;
    }

    public void setData(T data) {
        this.data = data;
    }
    
    public void setIsSuccessful(boolean success) {
        isSuccessful = success;
    }
}
