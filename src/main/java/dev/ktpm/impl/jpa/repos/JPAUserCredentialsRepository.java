package dev.ktpm.impl.jpa.repos;

import java.util.Optional;
import java.util.UUID;

import dev.ktpm.impl.jpa.HibernateRuntime;
import dev.ktpm.impl.jpa.entities.UserCredentialsEntity;
import dev.ktpm.model.UserCredentials;
import dev.ktpm.repository.IUserCredentialsRepository;

public class JPAUserCredentialsRepository implements IUserCredentialsRepository {
	private final HibernateRuntime runtime;
	
	public JPAUserCredentialsRepository(HibernateRuntime runtime) {
		this.runtime = runtime;
	}
	
	@Override
	public Optional<UserCredentials> get(UUID userUUID) {
		try (var man = runtime.openEntityManager()) {
			UserCredentialsEntity entity = man.find(UserCredentialsEntity.class, userUUID);
			return entity == null ? Optional.empty() : Optional.of(entity.toModel());
		}
	}
	
	@Override
	public Optional<UserCredentials> getByUsername(String username) {
		try (var man = runtime.open()) {
			return man.eman.createQuery("""
				SELECT c FROM UserCredentialsEntity c
				JOIN UserEntity u ON u.uuid = c.userUUID
				WHERE u.username = :username
			""", UserCredentialsEntity.class)
			.setParameter("username", username)
			.getResultList().stream().findFirst().map(UserCredentialsEntity::toModel);
		}
	}
	
	@Override
	public void set(UserCredentials credentials) {
		try (var man = runtime.open()) {
			UserCredentialsEntity existing = man.eman.find(
				UserCredentialsEntity.class, credentials.userUUID()
			);
			if (existing == null) {
				man.eman.persist(UserCredentialsEntity.fromModel(credentials));
			} else {
				existing.setPasswordHash(credentials.passwordHash());
			}
		}
	}
}
