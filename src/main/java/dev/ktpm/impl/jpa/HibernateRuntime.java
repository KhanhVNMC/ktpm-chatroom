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
		String host = env("DB_HOST", "localhost");
		String port = env("DB_PORT", "5432");
		String database = env("DB_NAME", "ktpm_chat");
		String username = env("DB_USER", "postgres");
		String password = env("DB_PASSWORD", "123");

		Configuration configuration = new Configuration()
				.configure("hibernate.cfg.xml");

		configuration.setProperty(
				"hibernate.connection.url",
				"jdbc:postgresql://" + host + ":" + port + "/" + database
		);
		configuration.setProperty("hibernate.connection.username", username);
		configuration.setProperty("hibernate.connection.password", password);

		SessionFactory sessionFactory = configuration.buildSessionFactory();
		return new HibernateRuntime(sessionFactory);
	}

	private static String env(String name, String defaultValue) {
		String value = System.getenv(name);
		return value == null || value.isBlank() ? defaultValue : value;
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