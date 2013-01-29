package ie.wombat.ha.server;

import ie.wombat.ha.HibernateUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.annotations.GenericGenerator;

@Entity
public class Device {

	@Id
	@GeneratedValue(generator="increment")
	@GenericGenerator(name="increment", strategy = "increment")
	private Long id;
	
	@ManyToOne
	private Network network;
	
	/**
	 * 64 bit IEEE assigned unique device address
	 */
	private String address64;
	
	/**
	 * 16 bit network address
	 */
	private String address16;
	
	private String name;
	
	private Date lastContactTime;
	
	private String driverClassName;
	
	@OneToMany
	private List<EndPoint> endPoints = new ArrayList<EndPoint>();
	
	public  boolean isOk () {
		if (lastContactTime == null) {
			return false;
		}
		long age = System.currentTimeMillis() - lastContactTime.getTime();
		return ( age < 3600000L);
	}

	public void logEvent (int eventType, String eventName, String eventData) {
		LogRecord logRecord = new LogRecord();
		logRecord.setDevice(this);
		logRecord.setNetworkId(network.getId());
		
		logRecord.setType(eventType);
		logRecord.setName(eventName);
		logRecord.setData(eventData);
		
		EntityManager em = HibernateUtil.getEntityManager();
		
		//em.getTransaction().begin();
		em.persist(logRecord);
		//em.getTransaction().commit();
		
	}
	// 
	// Accessors
	//
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getAddress64() {
		return address64;
	}

	public void setAddress64(String address64) {
		this.address64 = address64;
	}
	
	

	public String getAddress16() {
		return address16;
	}

	public void setAddress16(String address16) {
		this.address16 = address16;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Date getLastContactTime() {
		return lastContactTime;
	}

	public void setLastContactTime(Date lastContactTime) {
		this.lastContactTime = lastContactTime;
	}

	public Network getNetwork() {
		return network;
	}

	public void setNetwork(Network network) {
		this.network = network;
	}

	public String getDriverClassName() {
		return driverClassName;
	}

	public void setDriverClassName(String driverClassName) {
		this.driverClassName = driverClassName;
	}

	public List<EndPoint> getEndPoints() {
		return endPoints;
	}

	public void setEndPoints(List<EndPoint> endPoints) {
		this.endPoints = endPoints;
	}

	
}
