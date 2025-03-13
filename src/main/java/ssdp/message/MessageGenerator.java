package ssdp.message;

import java.net.InetSocketAddress;

import ssdp.models.ServiceInfo;

public class MessageGenerator
{
	private static final String NEWLINE = SsdpConstants.NEWLINE;

    //Advertisement
	/*
	1.2.2 Device available - NOTIFY with ssdp:alive
	When a device is added to the network, it shall multicast discovery messages to advertise its
	root device, any embedded devices, and any services. Each discovery message shall contain
	four major components:
	a) A notification type (e.g., device type), sent in an NT (Notification Type) header field.
	b) A composite identifier for the advertisement, sent in a USN (Unique Service Name) header
	field.
	c) A URL for more information about the device (or enclosing device in the case of a service),
	sent in a LOCATION header field.
	d) A duration for which the advertisement is valid, sent in a CACHE-CONTROL header field.
	To advertise its capabilities, a device multicasts a number of discovery messages. Specifically,
	a root device shall multicast:
	*/
	/*
	 * The TTL for the IP packet should default to 2 and should be configurable.
	 */
	
	/**
	 * (THIS IS FOR THIS DEVICE)
	 * This is a ssdp:alive message. This message is sent periodically as a keep-alive like and for broadcasting
	 * the service that the device offers. The server must sent this message for advertising his services.
	 * <pre>
     * {@code
     * NOTIFY * HTTP/1.1
     * HOST: 239.255.255.250:1900
     * CACHE-CONTROL: max-age = seconds until advertisement expires
     * LOCATION: URL for UPnP description for root device
     * NT: notification type
     * NTS: ssdp:alive
     * SERVER: OS/version UPnP/2.0 product/version
     * USN: composite identifier for the advertisement
     * }
     * </pre>
	 */
	public static String notifyAliveMessage()
	{
		StringBuilder content = new StringBuilder();
		//NOTIFY * HTTP/1.1
        content.append(SsdpConstants.SL_NOTIFY).append(NEWLINE);
		//HOST: 239.255.255.250:1900
        content.append(SsdpConstants.HOST).append(NEWLINE);
        //CACHE-CONTROL: max-age = seconds until advertisement expires
        content.append(SsdpConstants.CACHE_CONTROL).append(SsdpConstants.CACHE_AGE).append(NEWLINE);
        //LOCATION: URL for UPnP description for root device
        content.append(SsdpConstants.LOCATION).append(NEWLINE);
        //NT: notification type
        content.append(SsdpConstants.NT_DEVICE).append(NEWLINE);//TODO Not sure if this is correct
        //NTS: ssdp:alive
        content.append(SsdpConstants.NTS.NTS_ALIVE).append(NEWLINE);
        //SERVER: OS/version UPnP/2.0 product/version
        content.append(SsdpConstants.SERVER_OS).append(NEWLINE);
        //USN: composite identifier for the advertisement
        content.append(SsdpConstants.getDefaultDeviceUSN()).append(NEWLINE);//TODO Not sure if this is correct
        
        //BOOTID.UPNP.ORG: number increased each time device sends an initial announce or an update message
        //CONFIGID.UPNP.ORG: number used for caching description information
        //SEARCHPORT.UPNP.ORG: number identifies port on which device responds to unicast M-SEARCH
        
        //This must end in a blank line.
        content.append(NEWLINE);
        return content.toString();
	}

	/**
     * This is a ssdp:alive message. This message is sent periodically as a keep-alive like and for broadcasting
     * the service that the device offers. The server must sent this message for advertising his services.
     * <pre>
     * {@code
     * NOTIFY * HTTP/1.1
     * HOST: 239.255.255.250:1900
     * CACHE-CONTROL: max-age = seconds until advertisement expires
     * LOCATION: URL for UPnP description for root device
     * NT: notification type
     * NTS: ssdp:alive
     * SERVER: OS/version UPnP/2.0 product/version
     * USN: composite identifier for the advertisement
     * }
     * </pre>
     */
    public static String notifyServiceAliveMessage(ServiceInfo service)
    {
        StringBuilder content = new StringBuilder();
        //NOTIFY * HTTP/1.1
        content.append(SsdpConstants.SL_NOTIFY).append(NEWLINE);
        //HOST: 239.255.255.250:1900
        content.append(service.host).append(":").append(service.port).append(NEWLINE);
        //CACHE-CONTROL: max-age = seconds until advertisement expires
        content.append(SsdpConstants.CACHE_CONTROL).append(SsdpConstants.CACHE_AGE).append(NEWLINE);
        //LOCATION: URL for UPnP description for root device
        content.append(service.location).append(NEWLINE);
        //NT: notification type
        content.append(service.NT).append(NEWLINE);//TODO Not sure if this is correct
        //NTS: ssdp:alive
        content.append(SsdpConstants.NTS.NTS_ALIVE).append(NEWLINE);
        //SERVER: OS/version UPnP/2.0 product/version
        content.append(SsdpConstants.SERVER_OS).append(NEWLINE);
        //USN: composite identifier for the advertisement
        content.append(service.USN).append(NEWLINE);//TODO Not sure if this is correct
        
        //BOOTID.UPNP.ORG: number increased each time device sends an initial announce or an update message
        //CONFIGID.UPNP.ORG: number used for caching description information
        //SEARCHPORT.UPNP.ORG: number identifies port on which device responds to unicast M-SEARCH
        
        //This must end in a blank line.
        content.append(NEWLINE);
        return content.toString();
    }
	
	/**
	 * (THIS IS FOR THIS DEVICE)
	 * When a device and its services are going to be removed from the network, the device should
	 * multicast an ssdp:byebye message corresponding to each of the ssdp:alive messages it
	 * multicasted that have not already expired. If the device is removed abruptly from the network,
	 * it might not be possible to multicast a message. As a fallback, discovery messages shall
	 * include an expiration value in a CACHE-CONTROL field value  if not readvertised,the discovery 
	 * message eventually expires on its own.<br>
	 * When the device starts a ssdp:byebye message should be sent before any other messages.
	 * <pre>
	 * {@code
	 * NOTIFY * HTTP/1.1
	 * HOST: 239.255.255.250:1900
	 * NT: notification type
	 * NTS: ssdp:byebye
	 * USN: composite identifier for the advertisement
	 * }
	 * </pre>
	 * @return
	 */
	public static String notifyByeByeMessage() 
	{
	    StringBuilder content = new StringBuilder();
	    //NOTIFY * HTTP/1.1
        content.append(SsdpConstants.SL_NOTIFY).append(NEWLINE);
        //HOST: 239.255.255.250:1900
        content.append(SsdpConstants.HOST).append(NEWLINE);
        //NT: notification type
        content.append(SsdpConstants.NT_DEVICE).append(NEWLINE);//TODO Not sure if this is correct
	    //NTS: ssdp:byebye
        content.append(SsdpConstants.NTS.NTS_BYEBYE).append(NEWLINE);
        //USN: composite identifier for the advertisement
        content.append(SsdpConstants.getDefaultDeviceUSN()).append(NEWLINE);//TODO Not sure if this is correct
        
        //BOOTID.UPNP.ORG: number increased each time device sends an initial announce or an update message
        //CONFIGID.UPNP.ORG: number used for caching description information

        //This must end in a blank line.
        content.append(NEWLINE);
        return content.toString();
	}

	/**
	 * This is a ssdp:update message. This messages is sent when a device changes. 
	 * When a client receives this, it must refresh it's data for this device.
	 * <pre>
	 * {@code
	 * NOTIFY * HTTP/1.1
	 * HOST: 239.255.255.250:1900
	 * LOCATION: URL for UPnP description for root device
	 * NT: notification type
	 * NTS: ssdp:update
	 * SERVER: OS/version UPnP/2.0 product/version
	 * USN: composite identifier for the advertisement
	 * }
	 * </pre>
	 * @return
	 */
	public static String notifyUpdateMessage()
	{
		StringBuilder content = new StringBuilder();
		//NOTIFY * HTTP/1.1
        content.append(SsdpConstants.SL_NOTIFY).append(NEWLINE);
		//HOST: 239.255.255.250:1900
        content.append(SsdpConstants.HOST).append(NEWLINE);
        //LOCATION: URL for UPnP description for root device
        content.append(SsdpConstants.LOCATION).append(NEWLINE);
        //NT: notification type
        content.append(SsdpConstants.NT_DEVICE).append(NEWLINE);//TODO Not sure if this is correct
        //NTS: ssdp:update
        content.append(SsdpConstants.NTS.NTS_UPDATE).append(NEWLINE);
        //SERVER: OS/version UPnP/2.0 product/version
        content.append(SsdpConstants.SERVER_OS).append(NEWLINE);
        //USN: composite identifier for the advertisement
        content.append(SsdpConstants.getDefaultDeviceUSN()).append(NEWLINE);//TODO Not sure if this is correct
        
        //BOOTID.UPNP.ORG: BOOTID value that the device has used in its previous announcements
        //CONFIGID.UPNP.ORG: number used for caching description information
        //NEXTBOOTID.UPNP.ORG: new BOOTID value that the device will use in subsequent announcements
            //Its field value shall be a non-negative 31-bit integer;
            //ASCII encoded, decimal, without leading zeros (leading zeroes, if present, shall be ignored by the recipient)
            //and shall be greater than the field value of the BOOTID.UPNP.ORG header field.
        
        //SEARCHPORT.UPNP.ORG: number identifies port on which device responds to unicast M-SEARCH
        
        
        //This must end in a blank line.
        content.append(NEWLINE);
        return content.toString();
	}
	
	//--------------------------------------------------------------------------------------------------------------------------------
	//Search
	
	/*
	 * When a control point desires to search the network for devices, it shall send a multicast 
	 * request with method M-SEARCH in the following format. Control points that know the address
	 * of a specific device are allowed to also use a similar format to send unicast requests with
	 * method M-SEARCH.<br>
	 * 
	 * Due to the unreliable nature of UDP, control points should send each M-SEARCH message
	 * more than once. As a fallback, to guard against the possibility that a device might not receive
	 * the M-SEARCH message from a control point, a device should re-send its advertisements
	 * periodically (see CACHE-CONTROL header field in NOTIFY with ssdp:alive above).<br>
	 */
	
	/**
	 * This is a ssdp:discover message (M-SEARCH).<br>
	 * When a client (control point) wants to search for a device, it must send a discover message.
	 * Search for any device and a device/server with the parameter ST (searchTarget)
	 * <pre>
	 * {@code
	 * M-SEARCH * HTTP/1.1
	 * HOST: 239.255.255.250:1900
	 * MAN: "ssdp:discover"
	 * MX: seconds to delay response (mx)
	 * ST: search target (searchTarget)
	 * USER-AGENT: OS/version UPnP/2.0 product/version
	 * CPUUID.UPNP.ORG: uuid of the control point
	 * }
	 * </pre>
	 * @param mx - seconds to delay response, default 2.
	 * @param searchTarget - specified in the SsdpConstants
	 * @return
	 */
	public static String mSearchRequestMessage_MultiCast(int mx, String searchTarget) 
	{
	      StringBuilder content = new StringBuilder();
	      //M-SEARCH * HTTP/1.1
	      content.append(SsdpConstants.SL_MSEARCH).append(NEWLINE);
	      //HOST: 239.255.255.250:1900
	      content.append(SsdpConstants.HOST).append(NEWLINE);
	      //MAN: "ssdp:discover"
	      content.append(SsdpConstants.MAN_DISCOVER).append(NEWLINE);
	      //MX: seconds to delay response
	      content.append(SsdpConstants.MX).append(mx).append(NEWLINE);
	      //ST: search target
	      if(!searchTarget.startsWith(SsdpConstants.ST))
	      {
	          content.append(SsdpConstants.ST);
	      }
	      content.append(searchTarget).append(NEWLINE);
	      //USER-AGENT: OS/version UPnP/2.0 product/version
	      content.append(SsdpConstants.getUserAgent()).append(NEWLINE);
	      //CPFN.UPNP.ORG: friendly name of the control point
	      //CPUUID.UPNP.ORG: uuid of the control point
	      content.append("CPUUID.UPNP.ORG:").append(SsdpConstants.deviceUUID).append(NEWLINE);
	      
	      //This must end in a blank line.
	      content.append(NEWLINE);
	      return content.toString();
	}
	
	//Is this method needed? I thought it may be useful.
	/*
     * When a control point desires to search the network for devices, it shall send a multicast 
     * request with method M-SEARCH in the following format. Control points that know the address
     * of a specific device are allowed to also use a similar format to send unicast requests with
     * method M-SEARCH.<br>
     * 
     * Due to the unreliable nature of UDP, control points should send each M-SEARCH message
     * more than once. As a fallback, to guard against the possibility that a device might not receive
     * the M-SEARCH message from a control point, a device should re-send its advertisements
     * periodically (see CACHE-CONTROL header field in NOTIFY with ssdp:alive above).<br>
     */
	/**
	 * This is a ssdp:discover message (M-SEARCH).<br>
     * When a client (control point) wants to search for a device, it must send a discover message.
     * Search for any device and a device/server with the parameter ST (searchTarget)
     * <pre>
     * {@code
     * M-SEARCH * HTTP/1.1
     * HOST: (address and port)
     * MAN: "ssdp:discover"
     * MX: seconds to delay response (mx)
     * ST: search target (searchTarget)
     * USER-AGENT: OS/version UPnP/2.0 product/version
     * }
     * </pre>
     * @param address
     * @param searchTarget - specified in the SsdpConstants
     * @return
     */
	public static String mSearchRequestMessage_UniCast(InetSocketAddress address, String searchTarget)
	{
	    return mSearchRequestMessage_UniCast(address.getHostString(), address.getPort(), searchTarget);
	}
	
    /*
     * When a control point desires to search the network for devices, it shall send a multicast 
     * request with method M-SEARCH in the following format. Control points that know the address
     * of a specific device are allowed to also use a similar format to send unicast requests with
     * method M-SEARCH.<br>
     * 
     * Due to the unreliable nature of UDP, control points should send each M-SEARCH message
     * more than once. As a fallback, to guard against the possibility that a device might not receive
     * the M-SEARCH message from a control point, a device should re-send its advertisements
     * periodically (see CACHE-CONTROL header field in NOTIFY with ssdp:alive above).<br>
     *
     */

	/**
     * This is a ssdp:discover message (M-SEARCH).<br>
     * When a client (control point) wants to search for a device, it must send a discover message.
     * Search for any device and a device/server with the parameter ST (searchTarget)
     * <pre>
     * {@code
     * M-SEARCH * HTTP/1.1
     * HOST: (address : port)
     * MAN: "ssdp:discover"
     * MX: seconds to delay response (mx)
     * ST: search target (searchTarget)
     * USER-AGENT: OS/version UPnP/2.0 product/version
     * }
     * </pre>
	 * @param address
	 * @param port
     * @param searchTarget - specified in the SsdpConstants
	 * @return
	 */
	public static String mSearchRequestMessage_UniCast(String address, int port, String searchTarget) 
    {
          StringBuilder content = new StringBuilder();
          //M-SEARCH * HTTP/1.1
          content.append(SsdpConstants.SL_MSEARCH).append(NEWLINE);
          //HOST: hostname:portNumber
          content.append(address).append(":").append(port).append(NEWLINE);
          //MAN: "ssdp:discover"
          content.append(SsdpConstants.MAN_DISCOVER).append(NEWLINE);
          //ST: search target
          if(!searchTarget.startsWith(SsdpConstants.ST))
          {
              content.append(SsdpConstants.ST);
          }
          content.append(searchTarget).append(NEWLINE);
          //USER-AGENT: OS/version UPnP/2.0 product/version
          content.append(SsdpConstants.getUserAgent()).append(NEWLINE);
          //This must end in a blank line.
          content.append(NEWLINE);
          return content.toString();
    }
	
	//Search Response
	/*
	 * To be found by a network search, a device shall send a unicast UDP response to the source
    IP address and port that sent the request to the multicast address. Devices respond if the ST
    header field of the M-SEARCH request is �ssdp:all�, �upnp:rootdevice�, �uuid:� followed by a
    UUID that exactly matches the one advertised by the device, or if the M-SEARCH request
    matches a device type or service type supported by the device. Multi-homed devices shall
    send the search response using the same UPnP-enabled interface on which the search
    request was received. The URL specified in the LOCATION field value shall specify an
    address that is reachable on that interface.
	 */
	/**
	 * This is a response to a discover message.
	 * <pre>
     * {@code
     * HTTP/1.1 200 OK
     * CACHE-CONTROL: max-age = seconds until advertisement expires
     * DATE: when response was generated
     * EXT:
     * LOCATION: URL for UPnP description for root device
     * SERVER: OS/version UPnP/2.0 product/version
     * ST: (searchTarget)
     * USN: composite identifier for the advertisement
     * }
     * </pre>
	 * @param searchTarget
	 * @param location
	 * @return
	 */
	public static String mSearchOK(String searchTarget, String location)
	{
        StringBuilder content = new StringBuilder();
        //HTTP/1.1 200 OK
        content.append(SsdpConstants.SL_OK).append(NEWLINE);
        //CACHE-CONTROL: max-age = seconds until advertisement expires
        content.append(SsdpConstants.CACHE_CONTROL).append(SsdpConstants.CACHE_AGE).append(NEWLINE);
        //DATE: when response was generated
        content.append(SsdpConstants.DATE).append(SsdpConstants.getDate()).append(NEWLINE);
        //EXT:
        content.append(SsdpConstants.EXT).append(NEWLINE);
        //LOCATION: URL for UPnP description for root device
        content.append(SsdpConstants.LOCATION).append(location.startsWith("http://") ? location : "http://"+location).append(NEWLINE);
        //SERVER: OS/version UPnP/2.0 product/version
        content.append(SsdpConstants.SERVER_OS).append(NEWLINE);
        //ST: search target
        if(!searchTarget.startsWith(SsdpConstants.ST))
        {
            content.append(SsdpConstants.ST);
        }
        content.append(searchTarget).append(NEWLINE);
        //USN: composite identifier for the advertisement
        content.append(SsdpConstants.getDefaultDeviceUSN()).append(NEWLINE);
        
        //BOOTID.UPNP.ORG: number increased each time device sends an initial announce or an update message
        //CONFIGID.UPNP.ORG: number used for caching description information
        //SEARCHPORT.UPNP.ORG: number identifies port on which device responds to unicast M-SEARCH
        // This must end in a blank line.
        content.append(NEWLINE);
        return content.toString();
	}

	//TODO Merge with the other mSearchRequesetMessage_Multicast. Maybe a builder pattern is needed?
	//Add the new java docs too.
	public static String mSearchRequestMessage_MultiCast(int mx, String searchTarget, int replyTcpPort, String controlPointName) 
	{
        if (!(replyTcpPort >= 49152 && replyTcpPort <= 65535)) 
        {
            System.err.println("The TCP port "+replyTcpPort+" is not in the valid range of 49152-65535.");
        }
        StringBuilder content = new StringBuilder();
        // M-SEARCH * HTTP/1.1
        content.append(SsdpConstants.SL_MSEARCH).append(NEWLINE);
        // HOST: 239.255.255.250:1900
        content.append(SsdpConstants.HOST).append(NEWLINE);
        // MAN: "ssdp:discover"
        content.append(SsdpConstants.MAN_DISCOVER).append(NEWLINE);
        // MX: seconds to delay response
        content.append(SsdpConstants.MX).append(mx).append(NEWLINE);
        // ST: search target
        if (!searchTarget.startsWith(SsdpConstants.ST)) {
            content.append(SsdpConstants.ST);
        }
        content.append(searchTarget).append(NEWLINE);
        // USER-AGENT: OS/version UPnP/2.0 product/version
        content.append(SsdpConstants.getUserAgent()).append(NEWLINE);
        // CPFN.UPNP.ORG: friendly name of the control point
        content.append("CPFN.UPNP.ORG:").append(controlPointName).append(NEWLINE);
        // TCPPORT.UPNP.ORG: the TCP port on which the device can reply to the search.
        content.append("TCPPORT.UPNP.ORG:").append(replyTcpPort).append(NEWLINE);
        // CPUUID.UPNP.ORG: uuid of the control point
        content.append("CPUUID.UPNP.ORG:").append(SsdpConstants.deviceUUID).append(NEWLINE);

        // This must end in a blank line.
        content.append(NEWLINE);
        return content.toString();
    }
}
