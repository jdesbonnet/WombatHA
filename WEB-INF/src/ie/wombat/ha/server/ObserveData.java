package ie.wombat.ha.server;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.GenericGenerator;

/**
 * A data point related to an {@link Observe} record. TODO: not happy with the names.
 * @author joe
 *
 */
@Entity
public class ObserveData {

	@Id
	@GeneratedValue(generator="increment")
	@GenericGenerator(name="increment", strategy = "increment")
	private Long id;
	
	private Date timestamp = new Date();
	
	@ManyToOne
	private Observe observe;
	
	private String data;


	//
	// Accessors
	//
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Observe getObserve() {
		return observe;
	}

	public void setObserve(Observe observe) {
		this.observe = observe;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
	
}
