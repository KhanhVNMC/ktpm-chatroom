package dev.ktpm.impl.jpa.repos;

import java.util.Optional;
import java.util.UUID;

import dev.ktpm.impl.jpa.HibernateRuntime;
import dev.ktpm.impl.jpa.entities.SessionEntity;
import dev.ktpm.model.Session;
import dev.ktpm.repository.ISessionRepository;

public class JPASessionRepository implements ISessionRepository {
	private final HibernateRuntime runtime;
	
	public JPASessionRepository(HibernateRuntime runtime) {
		this.runtime = runtime;
	}

	@Override
	public Session create(UUID userUUID) {
		long createdAt = System.currentTimeMillis();
		// TODO need consideration
		var session = new Session(
			UUID.randomUUID().toString(), userUUID, createdAt, createdAt + 86_400_000L
		);
		try (var man = runtime.open()) {
			man.eman.persist(SessionEntity.fromModel(session));
		}
		return session;
	}

	@Override
	public Optional<Session> get(String sessionID) {
		try (var man = runtime.openEntityManager()) {
			SessionEntity entity = man.find(SessionEntity.class, sessionID);
			return entity == null ? Optional.empty() : Optional.of(entity.toModel());
		}
	}

	@Override
	public void delete(String sessionID) {
		try (var man = runtime.open()) {
			man.eman.remove(man.eman.getReference(SessionEntity.class, sessionID));
		}
	}

	@Override
	public void deleteAllForUser(UUID userUUID) {
		try (var man = runtime.open()) {
			man.eman.createQuery("""
				DELETE FROM SessionEntity s WHERE s.userUUID = :user_uuid
			""")
			.setParameter("user_uuid", userUUID)
			.executeUpdate();
		}
	}
}
