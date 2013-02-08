package ie.wombat.ha.devices;



import java.io.IOException;

import org.apache.log4j.Logger;

import ie.wombat.ha.ByteFormatUtils;
import ie.wombat.ha.ZigBeeNIC;
import ie.wombat.ha.ZigBeePacket;
import ie.wombat.ha.ZigBeePacketFilter;

import ie.wombat.zigbee.AddressNotFoundException;
import ie.wombat.zigbee.ZigBeeCommand;
import ie.wombat.zigbee.ZigBeeCommandResponse;
import ie.wombat.zigbee.address.Address16;
import ie.wombat.zigbee.address.Address64;

import ie.wombat.zigbee.zcl.ZCLException;


/**
 * XBee Series 2 device driver running ZigBee firmware. Designed to be 
 * subclassed by specific applications
 * using the XBee module.
 * 
 * @author joe
 *
 */
public class XBeeSeries2 extends DeviceDriver  {

	private Logger log = Logger.getLogger (XBeeSeries2.class);
	

	public static final byte ADC = 2;
	public static final byte INPUT = 3;
	public static final byte HIGH_Z = 3;
	public static final byte LOW = 4;
	public static final byte HIGH = 5;
	
	private int frameId = 1;
	
	// AT command timeout in ms
	private int atQueryTimeout = 30000;
	private int ioSample;
	private long ioSampleTime;
	
	public XBeeSeries2(Address64 address64, Address16 address16, ZigBeeNIC nic) {
		super(address64, address16, nic);
		
	}


	public boolean isBatteryPowered () {
		return false;
	}

	public int getDigitalOutput (int line) throws IOException {
		// TODO: note this will require the UBee to be configured with
		// EP 0xE6 to have profileId 0xC105.
		if (line < 0 || line > 6) {
			log.warn("line must be 0 .. 6)");
			return -1;
		}
		byte[] command = new byte[2];
		command[0] = 0x44; // 'D'
		command[1] = (byte)(0x30 + line); // '0' .. '6'
		byte[] param = new byte[0];
		
		// Expecting response in following format:
		// 0x11, 0x44, {0x30|0x31|0x32|0x33}, 0x00, {0x04|0x05}
		
		byte[] response = execATQuery(command,param);
		System.err.println ("response to ATD" + line + ": " 
				+ ByteFormatUtils.byteArrayToString(response));
		
		return -1;
	}
	
	public int getIOSample () throws IOException {
		
		byte[] response = execATQuery("IS",null);
		
		if (response == null) {
			throw new IOException ("IS command: null response (!?)");
		}
		
		if (response.length < 8) {
			throw new IOException ("IS command: expecting 8 or more bytes response, received " + response.length);
		}
		
		if (response [1] != 0x49 || response[2] != 0x53 || response[3] != 0x00) {
			throw new IOException ("IS command:  frame-id 'I' 'S' 0x00 for first 4 bytes");

		}
		if (response[4] != 0x01) {
			throw new IOException ("IS command: expecting byte 4 to be 0x01, got " + response[4]);
		}
		
		// byte 4 always 0x01
		// bytes 5,6 : digital IO mask
		// bytes 7 : analog mask
		
		// if any digital IO enabled (ie bytes1,2 != 0x0000) then bytes 4,5 = IO state
		if (response.length < 10) {
			throw new IOException ("IS command: expecting IO data");
		}
		int ret;
		ret = response[8] << 8;
		ret |= (response[9] & 0xff);
		return ret;
		
	}
	
	public byte[] execATQuery(String command) throws IOException {
		return execATQuery(command,null);
	}
	public byte[] execATQuery(String command, byte[] params) throws IOException {
		byte[] atcommand = new byte[2];
		atcommand[0] = (byte)command.charAt(0);
		atcommand[1] = (byte)command.charAt(1);
		return execATQuery(atcommand,params);
	}
	
	/**
	 * Issue AT query. Wait for response.  
	 * TODO: is it the case that queries never have parameters? If so should I change
	 * this API to remove params from the async query version?
	 * See this blog post for more information:
	 * http://jdesbonnet.blogspot.com/2011/09/controlling-xbee-io-lines-with-zigbee.html
	 * 
	 * @param atcommand
	 * @param params
	 * @return
	 * @throws IOException
	 */
	public byte[] execATQuery(byte[] atcommand, byte[] params) throws IOException {
		
		int profileId = 0xC105;
		int clusterId = 0x0021;
		int srcEp = 230;
		int dstEp = 230;
		
		if (params == null) {
			params = new byte[0];
		}

		final int frameId = getNextFrameId() & 0xff;
		
		byte[] cmd = new byte[16+params.length];
		
		// ?
		cmd[0] = 0x00;
		cmd[1] = 0x32;
		cmd[2] = 0x00;
		
		cmd[3] = (byte)frameId;
		
		// cmd[4..11] = sending addr64
		// TODO: have to do something about this
		// Assuming the NIC is the coordinator. Wrong.
		Address64 senderAddr64 = Address64.COORDINATOR;
		System.arraycopy(senderAddr64.getBytesMSBF(), 0, cmd, 4, 8);
		// sending addr16
		cmd[12] = 0x00;
		cmd[13] = 0x00;
		
		// the AT command
		cmd[14] = atcommand[0];
		cmd[15] = atcommand[1];
		
		// AT command parameters
		for (int i = 0; i < params.length; i++) {
			cmd[16+i] = params[i];
		}
		
		
		ZigBeeCommand zcmd = new ZigBeeCommand(nic);
		zcmd.setAddress16(Address16.UNKNOWN);
		zcmd.setAddress64(getAddress64()); // EXP
		zcmd.setProfileId(profileId);
		zcmd.setClusterId(clusterId);
		zcmd.setSourceEndpoint(srcEp);
		zcmd.setDestinationEndpoint(dstEp);
		zcmd.setCommand(cmd);
		zcmd.setSuppressZCLHeader(true);
		zcmd.setSequenceId(ZigBeeCommand.NO_SEQUENCE);
		
		//final ThreadLocal<byte[]> xbeeResponse = new ThreadLocal<byte[]>();
		
		
		final Address16 xbeeAddr16 = getAddress16();
		
		zcmd.setCallback(new ZigBeeCommandResponse() {
			
			public void handleZigBeeCommandResponse(int status, Address16 addr16,
					ZigBeeCommand zcmd, byte[] payload) {
				
				
				System.err.println ("Got response to AT query: " + ByteFormatUtils.byteArrayToString(payload));
				if (payload!= null && payload.length >= 4) {
					int frameId = payload[0] & 0xff;
					System.err.println ("FrameID=" + frameId);
					System.err.println ("zcmd=" + zcmd);
					//xbeeResponse.set(payload);
					zcmd.setResponse(payload);
				} else {
					log.warn ("AT response: expecting payload >=4 bytes but received " + ByteFormatUtils.byteArrayToString(payload));
				}
				synchronized (zcmd) {
					zcmd.notifyAll();
				}
			}
		}, new ZigBeePacketFilter() {
			
			public boolean allow(ZigBeePacket packet) {
				
				System.err.println ("execATQuery(): Got packet to consider: " 
				+ ByteFormatUtils.byteArrayToString(packet.getPayload()));

				// Can't rely on Address16
				/*
				if (! packet.getSourceAddress16().equals(xbeeAddr16)) {
					System.err.println ("rejecting packet because addr16 = " + packet.getSourceAddress16());
					return false;
				}
				*/
				
				
				if (packet.getSourceEndPoint() != 230) {
					System.err.println ("rejecting packet because ep != 230, received " + packet.getSourceEndPoint());
					return false;
				}
				if (packet.getClusterId() != 0xA1) {
					System.err.println ("rejecting packet because clusterId != 0xA1, received " + packet.getSourceEndPoint());
					return false;
				}
				if (packet.getPayload().length < 3) {
					System.err.println ("rejecting packet because too short at " + packet.getPayload().length + " bytes");
					return false;
				}
				if ( (packet.getPayload()[0]&0xff) == frameId) {
					System.err.println ("FrameIDs match: both " + frameId);
				} else {
					System.err.println ("FrameID does not match. Expecting " + frameId 
							+ " but got " + (packet.getPayload()[0]&0xff));
					return false;
				}
				
				System.err.println ("looks like good XBee response packet");
				return true;
			}
		});
		
		System.err.println ("calling exec() on zcmd=" + zcmd);
		zcmd.exec();
				
		synchronized (zcmd) {
			try {
				zcmd.wait(atQueryTimeout);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		if (zcmd.getResponse() == null) {
			log.warn ("NO RESPONSE RECEIVED");
			throw new IOException ("timeout");
			//return null;
		}
		return zcmd.getResponse();
		
	}
	
	public void atCommand (String atCmd) throws IOException {
		atCommand(atCmd,null);
	}
	/**
	 * Send AT command asynchronously. 
	 * 
	 * @param atCmd
	 * @param params
	 * @throws IOException
	 */
	public void atCommand (String atCmd, byte[] params) throws IOException {
		
		byte[] atcommand = atCmd.getBytes();
		
		int profileId = 0xC105;
		int clusterId = 0x0021;
		int srcEp = 230;
		int dstEp = 230;
		
		if (params == null) {
			params = new byte[0];
		}

		final int frameId = getNextFrameId() & 0xff;
		
		byte[] cmd = new byte[16+params.length];
		cmd[0] = 0x00;
		cmd[1] = 0x32;
		cmd[2] = 0x00;
		cmd[3] = (byte)frameId;
		
		// TODO: fix hard coded addr64 address
		Address64 senderAddr64 = new Address64("00:13:A2:00:40:3c:15:5c");
		System.arraycopy(senderAddr64.getBytesMSBF(), 0, cmd, 4, 8);
		cmd[12] = 0x00;
		cmd[13] = 0x00;
		
	
		cmd[14] = atcommand[0];
		cmd[15] = atcommand[1];
		for (int i = 0; i < params.length; i++) {
			cmd[16+i] = params[i];
		}
		
		nic.sendZigBeeCommand(this.address64, Address16.UNKNOWN, clusterId, profileId, srcEp, dstEp, cmd);
	}
	
	
	public void setDigitalOutput(int line, boolean b) throws IOException {	
	
		log.info("setDigitalOutput(line=" + line + " ," + b + ")");
		
		// TODO: note this will require the UBee to be configured with 
		// EP 0xE6 to have profileId 0xC105.
		if (line < 0 || line > 6) {
			log.warn("line must be 0 .. 6)");
			return;
		}
		
		
		byte[] param = new byte[1];
		param[0] = b ? (byte)5 : (byte)4;
		log.info ("ATD"+line + param[0]);
		atCommand ("D"+line, param);
		
		/*
		byte[] param = new byte[4];
		param[0] = b ? (byte)5 : (byte)4;
		param[1] = 0;
		param[2] = 'A';
		param[3] = 'C';
		*/
		
		log.info ("ATD"+line + param[0]);
		atCommand ("D"+line, param);
		
		
		// ATAC is required for changes to DIO lines to take effect.
		
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// ignore
		}
		log.info("ATAC");
		atCommand("AC", null);
		
		
	}


	/**
	 * Send data to XBee running AT mode firmware. Data will be transmitted on the XBee's 
	 * UART TX pin.
	 * 
	 * @param data
	 * @throws IOException
	 */
	public void sendData(byte[] data) throws IOException {	
		log.info("sendData(" + ByteFormatUtils.byteArrayToString(data) + ")");
		int profileId = 0xC105;
		int clusterId = 0x0011;
		int srcEp = 232;
		int dstEp = 232;
		nic.sendZigBeeCommand(address64,Address16.UNKNOWN, clusterId, profileId, srcEp, dstEp, data);
	}

	
	
	public int getDIO(int line) {
		log.debug("polling XBee EP 0xE8,Cluster 0x94, Attr 0x0000");
		int i;
		try {
			i = getIntegerAttribute(0x0, 0x92, 10, 0xE8, line);
			log.debug ("response=" + i);
			return i;
		} catch (AddressNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ZCLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return -1;
	}


	@Override
	public void handleZigBeePacket(ZigBeePacket packet) {
		super.handleZigBeePacket(packet);

		byte[] payload = packet.getPayload();
		//log.debug ("handleZigBeePacket() payload=" + ByteFormatUtils.byteArrayToString(payload));
		
		Address16 srcAddr16 = packet.getSourceAddress16();
		int srcEp = packet.getSourceEndPoint();
		int clusterId = packet.getClusterId();
		
		if (srcEp == 232 && clusterId == 0x92 && srcAddr16.equals(this.address16) && payload.length >= 6 ) {
			//System.err.println ("****** IO SAMPLE ****** : " + ByteFormatUtils.byteArrayToString(payload));
			int sample;
			sample = payload[4] << 8;
			sample |= (payload[5] & 0xff);
			this.ioSample = sample;
			this.ioSampleTime = System.currentTimeMillis();
		}

	}
	
	protected int getNextFrameId () {
		return frameId++;
	}

	
	public void setTimeout (int timeout) {
		this.atQueryTimeout = timeout;
	}
	
	public int getLastIOSample () {
		return this.ioSample;
	}
	public long getLastIOSampleTime () {
		return this.ioSampleTime;
	}
	
}
