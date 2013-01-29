package ie.wombat.ha.server;


import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.GenericGenerator;

@Entity
public class LogRecord {

	public static final int TRACE = 1;
	public static final int DEBUG = 2;
	public static final int INFO = 3;
	public static final int WARN = 4;
	public static final int ERROR = 5;
	public static final int SEVERE = 6;
	
	public static final int DATA_INT = 0x101;
	
	public static final int APP_EVENT = 0x201;
	
	@Id
	@GeneratedValue(generator="increment")
	@GenericGenerator(name="increment", strategy = "increment")
	private Long id;
	
	// TODO: should this be a ManyToOne to the Network record?
	private Long networkId;
	
	private Date timestamp = new Date();
	
	@ManyToOne
	private Device device;
	
	private Integer endPoint;
	
	private Integer type;
	
	private String name;
	private String data;
	
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Long getNetworkId() {
		return networkId;
	}
	public void setNetworkId(Long networkId) {
		this.networkId = networkId;
	}
	public Date getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
	public Device getDevice() {
		return device;
	}
	public void setDevice(Device device) {
		this.device = device;
	}
	public Integer getEndPoint() {
		return endPoint;
	}
	public void setEndPoint(Integer endPoint) {
		this.endPoint = endPoint;
	}
	public Integer getType() {
		return type;
	}
	public void setType(Integer type) {
		this.type = type;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getData() {
		return data;
	}
	public void setData(String data) {
		this.data = data;
	}

	
	
	
}
