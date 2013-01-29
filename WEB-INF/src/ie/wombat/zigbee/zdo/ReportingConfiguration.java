package ie.wombat.zigbee.zdo;

//import ie.wombat.ha.ByteFormatUtils;

//import org.apache.log4j.Logger;

/**
 * TODO: this does not belong in the ZDO package.
 * 
 * See section ZCL specification 2.4.10, page 32, "Read Reporting Configuration Response Command"
 *  
 * @author joe
 *
 */
public class ReportingConfiguration {

	//private static Logger log = Logger.getLogger(ReportingConfiguration.class);
	
	private int status=-1;
	private int direction;
	private int dataType;
	private int minInterval;
	private int maxInterval;
	private int reportableChange;
	
	private int attributeId;
	
	// Example: 00 1a 09 status=00 direction=00 attrId={00 00} attrDataType=29 minInterval={78 00} maxInterval={10 0e} 00 00
	// TODO: assuming just one record
	public ReportingConfiguration () {
		
	}
	//public ReportingConfiguration (byte[] packet, int offset) {
	//}
	public void addPacket (byte[] packet, int offset) {
	
		//log.debug ("adding packet to " + this + ": "+ ByteFormatUtils.byteArrayToString(packet,offset,packet.length-offset));
		status = packet[offset+0]&0xff;
		direction = packet[offset+1]&0xff;
		
		attributeId = packet[offset+2] &0xff;
		attributeId |= (packet[offset+3]&0xff)<<8;
		
		dataType = packet[offset+4] & 0xff;
		
		minInterval = packet[offset+5] &0xff;
		minInterval |= (packet[offset+6]&0xff)<<8;
		
		maxInterval = packet[offset+7] &0xff;
		maxInterval |= (packet[offset+8]&0xff)<<8;
		
		
		// TODO: the length of this field depends on attrDataType
		reportableChange = packet[offset+9] &0xff;
		reportableChange |= (packet[offset+10]&0xff)<<8;
	}
/*
	public String toString () {
		return "status=0x" + Integer.toHexString(status)
				+ " minInterval=" + minInterval
				+ " maxInterval=" + maxInterval
			;
	}
*/
	//
	// Accessors
	//
	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public int getDirection() {
		return direction;
	}

	public void setDirection(int direction) {
		this.direction = direction;
	}

	public int getDataType() {
		return dataType;
	}

	public void setDataType(int dataType) {
		this.dataType = dataType;
	}

	public int getMinInterval() {
		return minInterval;
	}

	public void setMinInterval(int minInterval) {
		this.minInterval = minInterval;
	}

	public int getMaxInterval() {
		return maxInterval;
	}

	public void setMaxInterval(int maxInterval) {
		this.maxInterval = maxInterval;
	}

	public int getReportableChange() {
		return reportableChange;
	}

	public void setReportableChange(int reportableChange) {
		this.reportableChange = reportableChange;
	}

	public int getAttributeId() {
		return attributeId;
	}

	public void setAttributeId(int attributeId) {
		this.attributeId = attributeId;
	}
	
	
	
	
}
