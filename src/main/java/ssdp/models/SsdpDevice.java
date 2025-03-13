package ssdp.models;

import java.net.InetAddress;
import java.util.Date;

public final class SsdpDevice {
    /**
     * The device's UUID.
     */
    public String deviceUUID;
    
    /**
     * The last time this device was contacted.
     */
    public Date lastContacted;
    /**
     * When this device is said to expire.
     */
    public Date expiration;

    /**
     * The address used by the device.
     */
    public InetAddress address;
    
    public SsdpDevice() {
        
    }

    @Override
    public String toString() {
        return "SsdpDevice [deviceUUID=" + deviceUUID + ", lastContacted=" + lastContacted
                + ", expiration=" + expiration + ", address=" + address + "]";
    }

 
}
