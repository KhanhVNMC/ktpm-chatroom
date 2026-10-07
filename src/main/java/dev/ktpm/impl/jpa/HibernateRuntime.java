package dev.ktpm.impl.jpa;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

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

	@Override
	public void close() {
		sessionFactory.close();
	}
}
