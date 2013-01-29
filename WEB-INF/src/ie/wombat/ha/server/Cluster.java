package ie.wombat.ha.server;

import java.util.List;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.annotations.GenericGenerator;

@Entity
public class Cluster {
	
	public static final int OUTPUT = 0;
	public static final int INPUT = 1;
	
	@Id
	@GeneratedValue(generator="increment")
	@GenericGenerator(name="increment", strategy = "increment")
	private Long id;
	
	private int direction;
	
	
	@ElementCollection
	private List<Integer> attributeIds;
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public List<Integer> getAttributeIds() {
		return attributeIds;
	}
	public void setAttributeIds(List<Integer> attributeIds) {
		this.attributeIds = attributeIds;
	}
	public int getDirection() {
		return direction;
	}
	public void setDirection(int direction) {
		this.direction = direction;
	}
	
	
}
