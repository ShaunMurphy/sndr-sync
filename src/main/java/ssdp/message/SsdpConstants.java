package ssdp.message;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public final class SsdpConstants 
{
    /** {@value} */
    public static final String SSDP_ADDRESS = "239.255.255.250";//Site-local
    public static final String SSDP_ADDRESS_v6 = "FF05::C";//Site-local
    
    /** {@value #SSDP_ADDRESS} */
    public static InetAddress INET_SSDP_ADDRESS;
    /** {@value} */
    public static final int SSDP_PORT = 1900;
    
    public static final InetSocketAddress multicastIPv4Socket = new InetSocketAddress(SsdpConstants.SSDP_ADDRESS, SsdpConstants.SSDP_PORT);
    public static final InetSocketAddress multicastIPv6Socket = new InetSocketAddress(SsdpConstants.SSDP_ADDRESS_v6, SsdpConstants.SSDP_PORT);

    //Period characters in the Vendor Domain Name shall be replaced with hyphens in accordance with RFC 2141.
    /** {@value} */
    public static final String DOMAIN_NAME = "sync-sndr-com";
    /** {@value} */
    public static final String PRODUCT_NAME = "sndr.link";
    /** {@value} */
    public static final int PRODUCT_VERSION = 1;
    
    
    /** The current device's type. {@value #DEVICE_TYPES} */
    public static DEVICE_TYPES currentDevice;
    /** {@value} */
    public static InetAddress localAddress;
    /** This device's UUID. */
    public static UUID deviceUUID;
    /** The device's OS */    public static OS os;


    public static OS deviceOS;

    /**
     * Initializes the SSDP Constants. This will generate a type 1 UUID for the device.
     * @param localAddress
     * @param currentDevice
     */
    public static void initialize(InetAddress localAddress, DEVICE_TYPES currentDevice) {
        initialize(localAddress, currentDevice, null);
        //Generates and sets the device UUID. Should be type 1. 
        SsdpConstants.deviceUUID = UUIDGenerator.getInstance(localAddress).generateUUID();
    }

    /**
     * Initializes the SSDP Constants.
     * @param localAddress
     * @param currentDevice
     * @param deviceUUID
     */
    public static void initialize(InetAddress localAddress, DEVICE_TYPES currentDevice, String deviceUUID) {
        try {
            INET_SSDP_ADDRESS = InetAddress.getByName(SSDP_ADDRESS);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        SsdpConstants.localAddress = localAddress;
        SsdpConstants.currentDevice = currentDevice;

        if(deviceUUID != null) {
            //Set the device UUID. Should be type 1.
            SsdpConstants.deviceUUID = UUID.fromString(deviceUUID);
        }
        deviceOS = OS.getThisDeviceOS();
    }

    public enum OS {
        Windows, Mac, Linux, Android, Unknown;
        
        public static OS getThisDeviceOS() {
            // OS
            String os = System.getProperty("os.name").toLowerCase();        
            if (os.contains("win")) {
                return OS.Windows;
            } else if (os.contains("mac")) {
                return OS.Mac;
            } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
                return OS.Linux;
            } else {
                return OS.Unknown;
            }
        }
    }
    
    
    /** {@value} */
    public static final String MSEARCH = "M-SEARCH";
    /** {@value} */
    public static final String NOTIFY = "NOTIFY";
    /** {@value} */
    public static final String OK = "OK";

	//SSDP Start Line (pick one):
    /** {@value}*/
    public static final String SL_MSEARCH = "M-SEARCH * HTTP/1.1";
    /** {@value}<br>Method for sending notifications and events.*/
	public static final String SL_NOTIFY = "NOTIFY * HTTP/1.1";
	/** {@value}*/
    public static final String SL_OK = "HTTP/1.1 200 OK";
	
    //Message Header:
    /** {@value}<br>Field value contains multicast address and port reserved for SSDP by Internet Assigned Numbers Authority (IANA)*/
    public static final String HOST = "HOST: " + SsdpConstants.SSDP_ADDRESS + ":" + SsdpConstants.SSDP_PORT;
    /** {@value} Required for backwards compatibility with UPnP 1.0. (Header field name only; no field value.)*/
    public static final String EXT = "EXT:";
    /** {@value}*/
    public static final String DATE = "DATE:";
    /** {@value}*/
    public static final String MAN_DISCOVER = "MAN: \"ssdp:discover\"";
    /** {@value} seconds to delay response. Must be 1, 2, 3, 4, or 5 seconds. */
    public static final String MX = "MX:";
    /** {@value}*/
    public static final String NEWLINE = "\r\n";
    //Search Targets:
    /** {@value} search target.*/
    public static final String ST = "ST:";
    /** {@value} search target. Search for all devices and services.*/
    public static final String ST_ALL = ST+"ssdp:all";
    /** {@value} search target. Search for root devices only.*/
    public static final String ST_ROOT_DEVICE = ST+"upnp:rootdevice";
    /** {@value} search target. Search for a particular device. 
     * The UUID must be in the <b>mandatory uuid</b> format. Format: {@link #getUUID()}.*/
    public static final String ST_DEVICE_UUID(String deviceUUID) {return ST+"uuid:"+deviceUUID;}
    /** ST:urn:schemas-upnp-org:device:{type}:{version} search target. Respond once for each matching device, root or embedded.*/
    public static final String ST_DEVICE_TYPE_VERSION(DEVICE_TYPES type, int version) {
        return ST+"urn:schemas-upnp-org:device:"+type.toString()+":"+version; 
    };
    /** ST:urn:schemas-upnp-org:service:{type}:{version} search target. Respond once for each matching service type.*/
    public static final String ST_SERVICE_TYPE_VERSION(SERVICE_TYPES type, int version) {
        return ST+"urn:schemas-upnp-org:service:"+type.toString()+":"+version; 
    };
    /** ST:urn:{domain}:device:{type}:{version} search target. Respond once for each matching device, root or embedded.
     * <br>domain - vender domain name {@value #DOMAIN_NAME}
     * @param type
     * @param version
     * @return
     */
    public static final String ST_DOMAIN_DEVICE_TYPE_VERSION(DEVICE_TYPES type, int version) {
        String domain = DOMAIN_NAME;
        //Period characters in the Vendor Domain Name shall be replaced with hyphens in accordance with RFC 2141.
        if(domain.contains(".")) {
            domain = domain.replace(".", "-");
        }
        return ST+"urn:"+domain+":device:"+type.toString()+":"+version; 
    };
    /** ST:urn:{domain}:service:{type}:{version} search target. Respond once for each matching service type.
     * <br>domain - vender domain name {@value #DOMAIN_NAME}
     * @param type
     * @param version
     * @return
     */
    public static final String ST_DOMAIN_SERVICE_TYPE_VERSION(SERVICE_TYPES type, int version) {
        String domain = DOMAIN_NAME;
        //Period characters in the Vendor Domain Name shall be replaced with hyphens in accordance with RFC 2141.
        if(domain.contains(".")) {
            domain = domain.replace(".", "-");
        }
        return ST+"urn:"+domain+":service:"+type.toString()+":"+version; 
    };

    
    //Message Header Extensions:
    //vendor-defined header field names shall have the following format:
    //field-name = token "." domain-name
    //Where the domain-name shall be Vendor Domain Name, and in addition shall satisfy the token
    //format as defined in RFC 2616, clause 2.2.

    //Search:
    /** {@value}*/
    public static final String ST_ROOT = "ST:upnp:rootdevice";
    public static final String ST_DEVICE() {
        return "ST:uuid:"+deviceUUID;
    };

    //Notify:
    /**Seconds until advertisement expires. Should be greater than or equal to 1800 seconds. {@value}*/
    public static final int CACHE_AGE = 1800;
    /**Seconds until advertisement expires {@value #CACHE_AGE}<br>
    Field value shall have the max-age directive ("max-age=") followed by an integer that specifies the
	number of seconds the advertisement is valid. After this duration, control points should assume the device (or
	service) is no longer available; as long as a control point has received at least one advertisement that is still
	valid from a root device, any of its embedded devices or any of its services, then the control point can assume
	that all are available. The number of seconds should be greater than or equal to 1800 seconds (30 minutes). 
	Specified by UPnP vendor. Other directives shall NOT be sent and shall be ignored when received. */
    public static final String CACHE_CONTROL = "CACHE-CONTROL: max-age=";
    //Not sure if this is correct.
    private static String getLocation()
    {
		return localAddress != null ? localAddress.getHostAddress() : "";
    }
    /**LOCATION: URL for UPnP description for root device.{@value}<br>
    Field value contains a URL to the UPnP description of the root device. Normally the host portion
	contains a literal IP address rather than a domain name in unmanaged networks. Specified by UPnP vendor.
	Single absolute URL (see RFC 3986).*/
    public static final String LOCATION = "LOCATION:"+getLocation();
    
    /**NT: notification type {@value}*/
    public static final String NT = "NT:";
    /** {@value}<br>Sent once for root device.*/
    public static final String NT_ROOT = "NT:upnp:rootdevice";
    /** {@value}<br>Sent once for each device, root or embedded, where device-UUID is specified by the UPnP vendor. */
    public static final String NT_DEVICE = "NT:uuid:"+deviceUUID;
    
    /**NTS: ssdp:alive<br>
    Definitions of notification sub-type.*/
    public static enum NTS {
    	/** "NTS:ssdp:alive" */      
    	NTS_ALIVE("NTS:ssdp:alive"), 
    	/** "NTS:ssdp:byebye" */
    	NTS_BYEBYE("NTS:ssdp:byebye"), 
    	/** "NTS:ssdp:update" */
    	NTS_UPDATE("NTS:ssdp:update");
    	NTS(String value) { this.value = value; }
        private final String value;
        @Override
        public String toString() {return value;}
    };

    //TODO Check if this is correct for the Upnp version.
    /**SERVER: OS/version UPnP/2.0 product/version */
    public static String SERVER_OS = "SERVER: "+System.getProperty("os.name")+
            "/"+System.getProperty("os.version")+" UPnP/2.0 "+PRODUCT_NAME+"/"+PRODUCT_VERSION;
    /**USN: composite identifier for the advertisement*/

    //TODO Generate the different USNs similar to how you did the different ST search targets.
    /*  Unique Service Name (USN) - An identifier that is unique across all
     *  services for all time. It is used to uniquely identify a particular
     *  service in order to allow services of identical service type to be
     *  differentiated.
     *  <pre>
     *  A USN is formed by two parts : UUID and URN and they are not mandatory.  
     *  
     *  UUID : Universally Unique Identifier 
     *  URN  : Uniform Resource Name.
     *      
     *  A URN must respect this format : 
     *  urn:schemas-upnp-org:device:deviceType:ver
     *  or
     *  urn:domain-name:device:deviceType:ver
     */
    
    /**
     * Generates a USN in this format:<br>
     * USN: uuid:device-UUID::upnp:rootdevice<br>
     * For now, the deviceType is the platform.<br>
     * Field value contains Unique Service Name. Identifies a unique instance of a device or service.
     * @param deviceUUID - The device UUID.
     * @return
     */
    public static String getRootDeviceUUIDUSN(String deviceUUID)
    {
        //USN: uuid:device-UUID::urn:schemas-upnp-org:device:deviceType:ver
        StringBuilder USN = new StringBuilder("USN:");
        USN.append("uuid:").append(deviceUUID).append("::");
        USN.append("upnp:rootdevice");
        return USN.toString();
    }

    /**
     * Generates a USN in this format:<br>
     * USN: uuid:device-UUID::urn:schemas-upnp-org:device:deviceType:ver<br>
     * For now, the deviceType is the platform.<br>
     * Field value contains Unique Service Name. Identifies a unique instance of a device or service.
     * @param type - The device type.
     * @return
     */
    public static String getDeviceUSN(DEVICE_TYPES type)
    {
    	//USN: uuid:device-UUID::urn:schemas-upnp-org:device:deviceType:ver
    	StringBuilder USN = new StringBuilder("USN:");
    	USN.append("uuid:").append(deviceUUID).append("::");
    	USN.append("urn:schemas-upnp-org:device:");
    	//Device type
    	USN.append(type).append(":").append(PRODUCT_VERSION);
    	return USN.toString();
    }

    /**
     * Generates a USN in this format:<br>
     * USN: uuid:device-UUID::urn:{@value #DOMAIN_NAME}:device:deviceType:ver<br>
     * For now, the deviceType is the platform.<br>
     * Field value contains Unique Service Name. Identifies a unique instance of a device or service.
     * @param type - The device type.
     * @return
     */    
    public static String getDomainDeviceUSN(DEVICE_TYPES type)
    {
        //USN: uuid:device-UUID::urn:schemas-upnp-org:device:deviceType:ver
        StringBuilder USN = new StringBuilder("USN:");
        USN.append("uuid:").append(deviceUUID).append("::");
        USN.append("urn:").append(DOMAIN_NAME).append(":device:");
        //Device type
        USN.append(type).append(":").append(PRODUCT_VERSION);
        return USN.toString();
    }
    
    /**
     * Generates a USN using the current device type in this format:<br>
     * USN: uuid:device-UUID::urn:{@value #DOMAIN_NAME}:device:deviceType:ver<br>
     * For now, the deviceType is the platform.<br>
     * Field value contains Unique Service Name. Identifies a unique instance of a device or service.
     * @return
     */
    public static String getDefaultDeviceUSN()
    {
        return getDomainDeviceUSN(currentDevice);
    }

    /**
     * Generates a USN in this format:<br>
     * USN: uuid:device-UUID::urn:schemas-upnp-org:serice:serviceType:ver<br>
     * Field value contains Unique Service Name. Identifies a unique instance of a device or service.
     * @param serviceName - The service's name.
     * @param version - service version
     * @return
     */
    public static String getServiceUSN(String serviceName, int version)
    {
        StringBuilder USN = new StringBuilder("USN:");
        USN.append("uuid:").append(deviceUUID).append("::");
        USN.append("urn:schemas-upnp-org:service:");
        USN.append(serviceName).append(":").append(version);
        return USN.toString();
    }

    /**
     * Generates a USN in this format:<br>
     * USN: uuid:device-UUID::urn:{@value #DOMAIN_NAME}:service:serviceType:ver<br>
     * Field value contains Unique Service Name. Identifies a unique instance of a device or service.
     * @param serviceType - The service's type.
     * @param version - service version
     * @return
     */
    public static String getDomainServiceUSN(SERVICE_TYPES service, int version)
    {
        StringBuilder USN = new StringBuilder("USN:");
        USN.append("uuid:").append(deviceUUID).append("::");
        USN.append("urn:").append(DOMAIN_NAME).append(":service:");
        USN.append(service).append(":").append(version);
        return USN.toString();
    }
    
    /*
    Field value shall begin with the following "product tokens" (defined
    by HTTP/1.1). The first product token identifies the operating system in the form OS name/OS version, the
    second token represents the UPnP version and shall be UPnP/2.0, and the third token identifies the product
    using the form product name/product version. For example, "USER-AGENT: unix/5.1 UPnP/2.0  MyProduct/1.0".
     */
    //TODO Check if this is correct for the Upnp version.
    //USER-AGENT: unix/5.1 UPnP/2.0 MyProduct/1.0
    //USER-AGENT: OS name/OS UPnP/2.0 version product name/product version
    public static String getUserAgent()
    {
        StringBuilder userAgent = new StringBuilder("USER-AGENT: ");
        userAgent.append(System.getProperty("os.name")).append("/").append(System.getProperty("os.version"));
        userAgent.append(" UPnP/2.0 ");
        userAgent.append(PRODUCT_NAME).append("/").append(PRODUCT_VERSION);
        return userAgent.toString();
    }
    
    /**
     * Returns the current date in RFC 1123 Time Format.<br>
     * Format: EEE, dd MMM yyyy HH:mm:ss zzz<br>
     * Append "DATE:" before this for SSDP calls.
     * @return
     */
    public static String getDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
        String date = sdf.format(new Date());
        return date;
    }
    
    /**
     * Parses a date string in in RFC 1123 Time Format.<br>
     * Format: EEE, dd MMM yyyy HH:mm:ss zzz<br>
     * @param date
     * @return
     */
    public static Date parseDate(String date) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
        try {
            return sdf.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Custom device types that I came up with. They are currently not following any device type specifications.
     * It should be following this: http://upnp.org/specs/dm/UPnP-dm-ManageableDevice-v1-Device.pdf
     */
    public enum DEVICE_TYPES {
        /** "sndr_all" */
        ALL("sndr_all"),
        /** "sndr_android" */
        ANDROID("sndr_android"),
        /** "sndr_desktop" */
        DESKTOP("sndr_desktop"), 
        /** "sndr_ios" */
        IOS("sndr_ios"),
        /** "sndr_link" */
        LINK("sndr_link");
        DEVICE_TYPES(String value) {this.value = value;}
        private final String value;
        @Override
        public String toString() {return value;}
    }

    //Custom service names. (They are not defined by the UPnP Forum working committee)
    /**
     * TODO
     */
    public enum SERVICE_TYPES {
        KEY_ESCROW("key-escrow"),
        FILE_ESCROW("file-escrow"),
        NOTHING("");
        SERVICE_TYPES(String value) {this.value = value;}
        private final String value;
        @Override
        public String toString() {return value;}
    }

    //Util stuff
    /**
     * A type 1 UUID.<br>
     * Generates a type 1 uuid using a time stamp and MAC address. 
     * Will generate unique time based UUID where the next UUID is always greater then the previous.
     * @param localAddress 
     * @return
     */
    public static String generateType1UUID(InetAddress localAddress) 
    {
        return UUIDGenerator.getInstance(localAddress).generateUUID().toString();
    }
 
}
