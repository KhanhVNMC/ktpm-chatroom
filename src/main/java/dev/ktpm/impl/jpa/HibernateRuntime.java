package dev.ktpm.impl.jpa;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

public final class HibernateRuntime implements AutoCloseable {
	private final SessionFactory sessionFactory;
	
	private HibernateRuntime(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	public static HibernateRuntime bootstrap() {
		SessionFactory sessionFactory = new Configuration()
			.configure("hibernate.cfg.xml")
			.buildSessionFactory()
		;
		return new HibernateRuntime(sessionFactory);
	}
	
	public SessionFactory sessionFactory() {
		return sessionFactory;
	}
	
	public EntityManager openEntityManager() {
		return sessionFactory.createEntityManager();
	}
	
	public EntityManagerCtx open() {
		return new EntityManagerCtx(sessionFactory.createEntityManager());
	}

	@Override
	public void close() {
		sessionFactory.close();
	}
	
	public static class EntityManagerCtx implements AutoCloseable {
		public final EntityManager eman;
		private final EntityTransaction transaction;
		
		public EntityManagerCtx(EntityManager entityManager) {
			this.eman = entityManager;
			this.transaction = this.eman.getTransaction();
			this.transaction.begin();
		}
		
		@Override
		public void close() {
			transaction.commit();
			eman.close();
		}
	}
}
