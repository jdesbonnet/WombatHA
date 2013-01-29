package ie.wombat.ha.server;

import java.sql.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.GenericGenerator;

@Entity
public class Occupancy {

	@Id
	@GeneratedValue(generator="increment")
	@GenericGenerator(name="increment", strategy = "increment")
	private Long id;
	
	private Date timestamp;
	
	@ManyToOne
 	private Device device;
	
 	private boolean occupied;
 	
 	
 	
}
