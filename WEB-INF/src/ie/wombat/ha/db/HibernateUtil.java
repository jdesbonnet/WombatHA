package ie.wombat.ha.db;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.ejb.HibernateEntityManagerFactory;

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
public class HibernateUtil {

	private static Log log = LogFactory.getLog(HibernateUtil.class);

	// Must match the name of the persistence unit in persistence.xml
	private static final String PERSISTENCE_UNIT="gg-persistence-unit";

	
	private static final SessionFactory sessionFactory;

	static {
		try {
			// Create the SessionFactory
			
			EntityManagerFactory emf =
			       Persistence.createEntityManagerFactory(PERSISTENCE_UNIT);

			HibernateEntityManagerFactory hibEMF =
		        (HibernateEntityManagerFactory) emf;
			sessionFactory = hibEMF.getSessionFactory();

			
		} catch (Throwable ex) {
			// Make sure you log the exception, as it might be swallowed
			log.error("Initial SessionFactory creation failed.", ex);
			ex.printStackTrace();
			throw new ExceptionInInitializerError(ex);
		}
	}

	public static final ThreadLocal<Session> session = new ThreadLocal<Session>();

	public static Session currentSession() {
		Session s = (Session) session.get();
		// Open a new Session, if this Thread has none yet
		if (s == null) {
			s = sessionFactory.openSession();
			session.set(s);
		}
		return s;
	}

	public static void closeSession() {
		Session s = (Session) session.get();
		if (s != null)
			s.close();
		session.set(null);
	}
	
	public static boolean isSessionOpen () {
		return ( session.get() != null );
	}
	
	public static SessionFactory getSessionFactory() {
		return sessionFactory;
	}
}
