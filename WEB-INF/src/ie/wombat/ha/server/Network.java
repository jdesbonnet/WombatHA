package ie.wombat.ha.server;

import ie.wombat.ha.HibernateUtil;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.annotations.GenericGenerator;


@Entity
public class Network {

	@Id
	@GeneratedValue(generator="increment")
	@GenericGenerator(name="increment", strategy = "increment")
	private Long id;
	
	@ManyToOne
	private Account account;
	/**
	 * User given name/label for the network
	 */
	private String name;
	
	/** 
	 * NIC Driver class and parameters
	 */
	private String nicDriver;
	
	/**
	 * If set 'false' this network will not be initialized
	 */
	private boolean enabled;
	
	/**
	 * Devices registered on the network. TODO: this belongs in volatile table (?)
	 */
	@OneToMany (mappedBy = "network")
	private List<Device> devices = new ArrayList<Device>();
	
	@OneToMany (mappedBy = "network")
	private List<Command> commands = new ArrayList<Command>();
	
	@OneToMany 
	private List<Application> applications = new ArrayList<Application>();
	

	public void logEvent (int eventType, String eventName, String eventData) {
		LogRecord logRecord = new LogRecord();
		logRecord.setDevice(null);
		logRecord.setNetworkId(id);
		
		logRecord.setType(eventType);
		logRecord.setName(eventName);
		logRecord.setData(eventData);
		
		EntityManager em = HibernateUtil.getEntityManager();
		em.getTransaction().begin();
		em.persist(logRecord);
		em.getTransaction().commit();
		
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

	public Account getAccount() {
		return account;
	}

	public void setAccount(Account account) {
		this.account = account;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	

	public String getNicDriver() {
		return nicDriver;
	}

	public void setNicDriver(String nicDriver) {
		this.nicDriver = nicDriver;
	}

	public List<Device> getDevices() {
		return devices;
	}

	public void setDevices(List<Device> devices) {
		this.devices = devices;
	}

	public List<Command> getCommands() {
		return commands;
	}

	public void setCommands(List<Command> commands) {
		this.commands = commands;
	}

	public List<Application> getApplications() {
		return applications;
	}

	public void setApplications(List<Application> applications) {
		this.applications = applications;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	

}
