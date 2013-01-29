package ie.wombat.ha;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.log4j.Logger;


/*
 * Created on May 22, 2005
 *
 
 */

/**
 * Scope for startup time optimization. Eg this
 * posting: http://forum.hibernate.org/viewtopic.php?p=2262489
 * http://www.hibernate.org/300.html
 * 
 * @author joe
 *  
 */
public class HibernateUtilOld {

	private static Logger log = Logger.getLogger(HibernateUtilOld.class);

	// Must match the name of the persistence unit in persistence.xml
	private static final String PERSISTENCE_UNIT="ie.wombat.ha.server";

	
	private static final EntityManagerFactory emf;

	static {
		try {
			// Create the EntityManagerFactory
			emf = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT);	
		} catch (Throwable ex) {
			// Make sure you log the exception, as it might be swallowed
			log.error("Initial SessionFactory creation failed.", ex);
			ex.printStackTrace();
			throw new ExceptionInInitializerError(ex);
		}
	}

	public static final ThreadLocal<EntityManager> entityManager = new ThreadLocal<EntityManager>();

	public static EntityManager currentEntityManager() {
		
		EntityManager em = (EntityManager) entityManager.get();
		// Open a new Session, if this Thread has none yet
		if (em == null) {
			em = emf.createEntityManager();
			em.getTransaction().begin();
			entityManager.set(em);
		}
		return em;
	}

	public static void closeEntityManager() {
		EntityManager em = (EntityManager) entityManager.get();
		if (em != null) {
			em.close();
		}
		entityManager.set(null);
	}
	
	public static boolean isSessionOpen () {
		return ( entityManager.get() != null );
	}
	
	
}
