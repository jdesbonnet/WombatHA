package ie.wombat.ha;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.log4j.Logger;


public class HibernateUtil {

	private static Logger log = Logger.getLogger(HibernateUtil.class);
	
	private static  EntityManagerFactory emf = Persistence.createEntityManagerFactory("ie.wombat.ha.server");
	//private static  EntityManager entityManager = buildEntityManager();
	public static final ThreadLocal<EntityManager> entityManager = new ThreadLocal<EntityManager>();

	private static EntityManager buildEntityManager() {
		try {
			emf = Persistence.createEntityManagerFactory("ie.wombat.ha.server");
			EntityManager em = emf.createEntityManager();
			
			log.debug("Created new EntityManagerFactory " + emf);

			return em;
			
		} catch (Throwable ex) {
			// Make sure you log the exception, as it might be swallowed
			System.err.println("Initial SessionFactory creation failed." + ex);
			throw new ExceptionInInitializerError(ex);
		}
	}
/*
	public static EntityManager getEntityManager_old() {
		log.debug ("getEntityManager()");
		if (!entityManager.isOpen()) {
			log.debug("EM " + entityManager + " not open. Creating a new one.");
			entityManager = emf.createEntityManager();
			log.debug ("New EM " + entityManager);
		}
		return entityManager;
	}
*/
public static EntityManager getEntityManager() {
		
		EntityManager em = (EntityManager) entityManager.get();
		// Open a new Session, if this Thread has none yet
		if (em == null || !em.isOpen()) {
			em = emf.createEntityManager();
			//em.getTransaction().begin();
			entityManager.set(em);
		}
		
		
		return em;
	}
}
