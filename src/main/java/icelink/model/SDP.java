package icelink.model;

import java.util.Date;

public final class SDP {   
    /** The UUID of the device to connect to. */
    public String remoteDeviceUuid;
    /**The UUID provided by the server for the connection.*/
    public String connectionUuid;
    /** The timeout for this connection. */
    public Date timeout;
    
    private final Offer offer;
    private final Answer answer;
    public SDP() {
        this.offer = new Offer();
        this.answer = new Answer();
    }
    
    
    public class Offer {
        private Offer() {}
        public String json;
        public byte[] encryptedJson;
        public String encryptedKey;
        public byte[] signature;
        public byte[] hmac;
    }
    
    public class Answer {
        private Answer() {}
        public String json;
        public byte[] encryptedJson;
        public String encryptedKey;
        public byte[] signature;
        public byte[] hmac;
    }
    
    
    public final Offer offer() {
        return this.offer;
    }
    
    public final Answer answer() {
        return this.answer;
    }
}