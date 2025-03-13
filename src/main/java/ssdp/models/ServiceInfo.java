package ssdp.models;

//import com.sndr.sync.ssdp.jobs.ServiceSearchTask;

public class ServiceInfo 
{
    /** Multicast IP */
    public final String host;
    /** Multicast Port */
    public final int port;
    /** Notification Type */
    public final String NT;
    /** URL for more information */
    public final String location;
    /** Unique Service Name */
    public final String USN;
    /** Uniform Resource Name */
    public final String URN;
    
    //TODO How should these fields be set?
    //public ServiceSearchTask serviceProcessor;
    
    public ServiceInfo(String host, int port, String NT, String location, String USN) 
            throws IllegalArgumentException {
        this.host = host;
        this.port = port;
        this.NT = NT;
        this.location = location;
        
        int index = USN.indexOf("::");
        if(index < 0) {
            throw new IllegalArgumentException("Invalid USN");
        }
        this.USN = USN;
        this.URN = USN.substring(index+2);
    }
    
    /** 
     * This is the class that will handle the requests for this service.
     * @param processor
     */
    //public void setServiceProcessingClass(ServiceSearchTask processor) {
    //    this.serviceProcessor = processor;
    //}
}
