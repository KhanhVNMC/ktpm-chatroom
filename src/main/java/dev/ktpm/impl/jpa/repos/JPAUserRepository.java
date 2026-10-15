package dev.ktpm.impl.jpa.repos;

import java.util.Optional;
import java.util.UUID;

import dev.ktpm.impl.jpa.HibernateRuntime;
import dev.ktpm.impl.jpa.entities.UserEntity;
import dev.ktpm.model.User;
import dev.ktpm.repository.IUserRepository;

public class JPAUserRepository implements IUserRepository {
	private final HibernateRuntime runtime;
	
	public JPAUserRepository(HibernateRuntime runtime) {
		this.runtime = runtime;
	}
	
	@Override
	public Optional<User> get(UUID userUUID) {
		try (var man = runtime.openEntityManager()) {
			UserEntity entity = man.find(UserEntity.class, userUUID);
			return entity == null ? Optional.empty() : Optional.of(entity.toModel());
		}
	}
	
	@Override
	public Optional<User> getByUsername(String username) {
		try (var man = runtime.open()) {
			return man.eman.createQuery("""
				SELECT u FROM UserEntity u WHERE u.username = :username
			""", UserEntity.class)
			.setParameter("username", username)
			.getResultList().stream().findFirst()
			.map(UserEntity::toModel);
		}
	}
	
	@Override
	public User create(User user) {
		try (var man = runtime.open()) {
			var entity = UserEntity.fromModel(user);
			man.eman.persist(entity);
			man.eman.flush();
			return entity.toModel();
		}
	}
	
	@Override
	public User update(User user) {
		try (var man = runtime.open()) {
			UserEntity existing = man.eman.find(UserEntity.class, user.uuid());
			if (existing == null) {
				return null;
			}
			existing.setUsername(user.username());
			existing.setDisplayName(user.displayName());
			existing.setAvatar(user.avatar());
			existing.setPresence(user.presence());
			existing.setStatusText(user.statusText());
			return existing.toModel();
		}
	}
	
	@Override
	public void delete(UUID userUUID) {
		try (var man = runtime.open()) {
			man.eman.remove(man.eman.getReference(UserEntity.class, userUUID));
		}
	}
}
