package ssdp.message;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.UUID;

//UUID generation
/*UUIDs are 128 bit numbers that shall be formatted as s pecified by the following grammar
(taken from [1]):
UUID = 4 * <hexOctet> �-� 2 * <hexOctet> �-� 2 * <hexOctet> �-� 2 * <hexOctet> �-� 6 * <hexOctet
hexOctet = <hexDigit> <hexDigit>
hexDigit = �0�|�1�|�2�|�3�|�4�|�5�|�6�|�7�|�8�|�9�|�a�|�b�|�c�|�d�|�e�|�f�|�A�|�B�|�C�|�D�|�E�|�F�
The following is an example of the string representation of a UUID:
"2fac1234-31f8-11b4-a222-08002b34c003"*/

/*
 UUIDs are allowed to be generated using any suitable generation algorithm2 that satisfies the
following requirements:
a) It is very unlikely to duplicate a UUID generated from some other resource.
b) It maps down to a 128-bit number.
c) UUIDs shall remain fixed over time.
The following UUID generation algorithm is recommended:
Time & MAC-based algorithm as specified in [1], where the UUID is generated once and
stored in non-volatile memory if available.
*/

/**
 * A class used to generate a type 1 UUID.
 * @author shaun
 */
public class UUIDGenerator {

	private static final Object lock = new Object();
	private static long lastTime;
	private static long clockSequence = 0;
	private final long hostIdentifier;
	
    //Offset to move from 1/1/1970, which is 0-time for Java, to Gregorian
    // 0-time 10/15/1582, and multiplier to go from 100nsec to msec units
    private static final long GREG_OFFSET = 0xB1D069B5400L;
    private static final long MILLI_MULT = 10000L;
	private static UUIDGenerator instance;
    private static InetAddress localAddress;

    private UUIDGenerator() {
        hostIdentifier = getMACAddressLong();
    }
    
    public static UUIDGenerator getInstance(InetAddress thisDeviceLocalAddress) {
        localAddress = thisDeviceLocalAddress;
    	if(instance == null) {
    		instance = new UUIDGenerator();
    	}
    	return instance;
    }

    
	/**
	 * Generates a type 1 uuid using a time stamp and MAC address.
	 * Will generate unique time based UUID where the next UUID is 
	 * always greater then the previous.
	 */
	public final UUID generateUUID() {
		return instance.generateIdFromTimestamp();
	}

	private final UUID generateIdFromTimestamp()
	{
		long clockSequenceHi = clockSequence;  
		clockSequenceHi <<=48;	
		long lsb = clockSequenceHi | hostIdentifier;
		
		long time = instance.getUUIDTime();
		return new UUID(time, lsb);
	}
	
	/**
	 * Gets the appropriately modified time stamp for the UUID. Must be called
     * from a synchronized block.
     * Time stamp is measured in 100-nanosecond units since midnight, October 15, 1582 UTC. 
     * (the Gregorian change offset)
	 * @return long time stamp in 100ns intervals since the Gregorian change offset.
	 */
	private long getUUIDTime()
	{
        long currentTime = (System.currentTimeMillis() + GREG_OFFSET) * MILLI_MULT;
		long time;

		synchronized (lock) {
			if (currentTime > lastTime) 
			{
				lastTime = currentTime;
				clockSequence = 0;
			}
			else 
			{ 
				++clockSequence; 
			}
		}
		time = currentTime;
		//low Time
		time = currentTime << 32;
		//mid Time
		time |= ((currentTime & 0xFFFF00000000L) >> 16);
		//hi Time
		time |= 0x1000 | ((currentTime >> 48) & 0x0FFF);
		return time;
	}

	/**
	 * Gets the MAC address of the local host ip address. Returns byte[6] array.
	 * @return
	 */
	private final static byte[] getMACAddress() 
	{
		byte[] mac = new byte[6];
		try
		{
			//InetAddress localAddress = InetAddress.getLocalHost();
			//System.out.println("Current IP address : " + ip.getHostAddress());
			//This will get the mac address of the network device being used by
			//the ip host address above. Wifi card, ethernet card etc.

			NetworkInterface network = NetworkInterface.getByInetAddress(localAddress);
			mac = network.getHardwareAddress();
		}
		catch (SocketException e)
		{
			e.printStackTrace();
			//TODO Use a logger here.
		}
		//System.out.println("MAC RAW "+Arrays.toString(mac));
		return mac;
	}
	
	/**
	 * Gets the MAC address of the local host ip address. Returns a hex-long mac address..
	 * @return
	 */
	private final static long getMACAddressLong() 
	{
		long output = 0;
		byte[] mac = getMACAddress();
		// Converts array of unsigned bytes to an long
		if (mac != null)
		{
			for (int i = 0; i < mac.length; i++)
			{
				output <<= 8;
				output ^= (long) mac[i] & 0xFF;
			}
		}
		return output;
	}
	
	//Conversion methods:
	/**
	 * Converts a UUID (Type 1) time stamp into a Java time stamp.<br>
	 * UUID time stamp is measured in 100-nanosecond units since midnight, October 15, 1582 UTC. 
     * (the Gregorian change offset)<br>
     * Java time is measured in 1-millisecond units since "the epoch", January 1, 1970, 00:00:00 GMT.
	 * @param uuid
	 * @return java based time stamp
	 */
	public long getTimeFromUUID(UUID uuid)
	{
		long uuidTime = uuid.timestamp();
		return (uuidTime / MILLI_MULT) - GREG_OFFSET;
	}

	/**
	 * Returns a MAC address from a uuid.
	 * @param uuid
	 * @return
	 */
	public String getMACFromUUID(UUID uuid)
	{
		long lsb = uuid.getLeastSignificantBits();
		byte[] mac = new byte[] {
                (byte)((lsb >> 40) & 0xff),
                (byte)((lsb >> 32) & 0xff),
                (byte)((lsb >> 24) & 0xff),
                (byte)((lsb >> 16) & 0xff),
                (byte)((lsb >> 8 ) & 0xff),
                (byte)((lsb >> 0) & 0xff)
        };
		//Convert the byte array into a string.
		String macAddress;
    	StringBuilder sb = new StringBuilder();
		for (int i = 0; i < mac.length; i++) {
			sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));		
		}
		macAddress = sb.toString();
		return macAddress;
	}
}