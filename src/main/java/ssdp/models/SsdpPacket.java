package ssdp.models;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import ssdp.message.SsdpConstants;

public final class SsdpPacket {
    private final DatagramPacket packet;
    private final InetAddress remoteAddress;
    private final int remotePort;
    private String method = null;
    private String version = null;
    private URI requestURI;

    private Data data = null;
    
    public SsdpPacket(DatagramPacket packet) {
        this.packet = packet;
        this.remoteAddress = packet.getAddress();
        this.remotePort = packet.getPort();
        parseToMap();
    }
    
    private LinkedHashMap<String, String> map = new LinkedHashMap<>();
    /**
     * Parses the packet data into a map.
     */
    private final void parseToMap() {
        byte[] data = packet.getData();
        //The data length may be longer than the packet length. This happens when the array
        //allocated is larger than the packet's required length.
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data, 0, packet.getLength());) {
            //Scanner scanner = new Scanner(bais);
            //scanner.nextLine();
    
            ByteBuffer buffer = ByteBuffer.allocate(256);
            String line = readLine(buffer, bais);
            //Parse the first line.
            String[] split = line.split(" ");
            if (split.length != 3) {
                System.err.println("Failed to parse.");
                return;
            }
            for(int i=0; i<3; i++) {
                String test = split[i].trim();
                if(test.startsWith("HTTP")) {
                    version = test; 
                } else if(test.equals("NOTIFY") || test.equals("M-SEARCH") || test.equals("OK")) {
                    method = test;
                }
            }

            //Parse the other fields.
            while(!(line = readLine(buffer, bais)).isEmpty()) {
                //Split line at :
                int index = line.indexOf(':');
                if(index < 0) {
                    System.out.println("Cannot parse "+line);
                }
                map.put(line.substring(0, index).trim(), line.substring(index + 1).trim());
            }      
            bais.close();
            
            //Set the Request URI.
            URI request = URI.create(split[1]);
            String hostHeader = map.get(SsdpConstants.HOST);
            if (!request.equals(URI.create("*")) && hostHeader != null) {
                if (!hostHeader.startsWith("http://")) {
                    hostHeader = "http://".concat(hostHeader);
                }
                request = URI.create(hostHeader).resolve(request);
            }
            this.requestURI = request;            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Reads one line of the buffer at a time.
     * @param buffer
     * @param i
     * @return
     */
    private final String readLine(ByteBuffer buffer, ByteArrayInputStream i) {
        buffer.clear();
        int b = i.read();
        if(b == '\n' || b == '\r') {
            b = i.read();
        }
        while(b > 0) {
            //If reached the end of the line.
            if(b == '\n' || b == '\r') {
                break;
            }
            buffer.put((byte)b);
            
            //Read the next byte.
            b = i.read();
        }
        //A line was read.
        buffer.flip();
        String line = new String(buffer.array(), 0, buffer.limit());
        buffer.compact();
        return line;
    }

    public final Data getData() {
        if(data == null) {
            parseData();
        }
        return data;
    }

    public boolean isMSearch() {
        return this.method.equals(SsdpConstants.MSEARCH);
    }

    public boolean isNotify() {
        return this.method.equals(SsdpConstants.NOTIFY);
    }

    public boolean isOK() {
        return this.method.equals(SsdpConstants.OK);
    }  

    public final InetAddress getAddress() {
        return this.remoteAddress;
    }
    
    public final int getPort() {
        return this.remotePort;
    }
    
    public final InetSocketAddress getSocketAddress() {
        return new InetSocketAddress(getAddress(), getPort());
    }
    
    public final class Data {
        /** The remote address where this packet came from. */
        public final InetAddress remoteAddress;
        /** The remote port where this packet came from. */
        public final int remotePort;
        
        public String SL = null; // Start line
        public String host = null; // Address and port
        // public int port = -1;
        public String cacheControl = null;
        public Date date = null;
        public String location = null;
        public int MX = -1; // Time-to-live
        public String MAN = null;
        public String NT = null; // Notification type
        public String NTS = null; // Notification sub-type
        public String server = null;
        public String ST = null; // Search target
        public String userAgent = null;
        public String USN = null;
        
        //Other fields
        /** TCPPORT.UPNP.ORG: A control point can request that a device replies to a TCP port on the control point. */
        public Integer TCPPORT;
        /** CPFN.UPNP.ORG: Friendly name of the control point */
        public String CPFN;
        /** CPUUID.UPNP.ORG: UUID of the control point */
        public String CPUUID;
        /** BOOTID.UPNP.ORG: Number increased each time device sends an initial announce or an update message.*/
        public Integer BOOTID;
        /** CONFIGID.UPNP.ORG: number used for caching description information.*/
        public Integer CONFIGID;
        /** SEARCHPORT.UPNP.ORG: number identifies port on which device responds to unicast M-SEARCH*/
        public Integer SEARCHPORT;

        //If the request was malformed.
        boolean malformedData = false;
        
        private Data(InetAddress remoteAddress, int remotePort) {
            this.remoteAddress = remoteAddress;
            this.remotePort = remotePort;
        }
        
        @Override
        public String toString() {
            return "Data [SL=" + SL + ", host=" + host + ", cacheControl=" + cacheControl
                    + ", date=" + date + ", location=" + location + ", MX=" + MX + ", MAN=" + MAN
                    + ", NT=" + NT + ", NTS=" + NTS + ", server=" + server + ", ST=" + ST
                    + ", userAgent=" + userAgent + ", USN=" + USN + ", TCPPORT=" + TCPPORT + ", CPFN="
                    + CPFN + ", CPUUID=" + CPUUID + ", BOOTID=" + BOOTID + ", CONFIGID=" + CONFIGID
                    + ", SEARCHPORT=" + SEARCHPORT + "]";
        }
        
        /**
         If there is an error with the search request (such as an invalid field value in the MAN header
        field, a missing MX header field, or other malformed content), the device shall silently discard
        and ignore the search request; sending of error responses is PROHIBITED due to the
        possibility of packet storms if many devices send an error response to the same request. 
        */
        public boolean checkMalformedRequest() {
            malformedData &= (MX > -1);
            //TODO
            return malformedData;
        }
    }
    
    /**
     * Parses the data from the map.
     */
    private final void parseData() {
        Data data = new Data(this.remoteAddress, this.remotePort);
        this.data = data;
        data.SL = (isNotify() ? SsdpConstants.NOTIFY : isMSearch() ? SsdpConstants.MSEARCH : isOK() ? SsdpConstants.OK : "")+" "+requestURI+" "+version;
        for(Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey().toUpperCase();
            String value = entry.getValue();
            if (key.equals("HOST")) {
                data.host = value;
            } else if (key.equals("SERVER")) {
                data.server = value;
            } else if (key.equals("NT")) {
                data.NT = value;
            } else if (key.equals("NTS")) {
                data.NTS = value;
            } else if (key.equals("CACHE-CONTROL")) {
                // data.cacheControl = Integer.valueOf(value.substring(value.indexOf("=")+1));
                data.cacheControl = value;
            } else if (key.equals("LOCATION")) {
                data.location = value;
            } else if (key.equals("USN")) {
                data.USN = value;
            } else if (key.equals("ST")) {
                data.ST = value;
            } else if (key.equals("MX")) {
                data.MX = Integer.valueOf(value);
            } else if (key.equals("MAN")) {
                data.MAN = value;
            } else if (key.equals("USER-AGENT") || key.equals("X-USER-AGENT")) {
                data.userAgent = value;
            } else if (key.equals("DATE")) {
                data.date = SsdpConstants.parseDate(value);
            } else if(key.equals("TCPPORT.UPNP.ORG")) {
                data.TCPPORT = Integer.valueOf(value);
            } else if(key.equals("CPUUID.UPNP.ORG")) {
                data.CPUUID = value;
            } else if(key.equals("CPFN.UPNP.ORG")) {
                data.CPFN = value;
            } else if(key.equals("BOOTID.UPNP.ORG")) {
                data.BOOTID = Integer.valueOf(value);
            } else if(key.equals("CONFIGID.UPNP.ORG")) {
                data.CONFIGID = Integer.valueOf(value);
            } else if(key.equals("SEARCHPORT.UPNP.ORG")) {
                data.SEARCHPORT = Integer.valueOf(value);
            } else if(key.equals("EXT") || key.equals("CONTENT-LENGTH")) {
                //Useless options
            }
        }
        //The map isn't needed anymore.
        this.map.clear();
        this.map = null;
    }
    
}