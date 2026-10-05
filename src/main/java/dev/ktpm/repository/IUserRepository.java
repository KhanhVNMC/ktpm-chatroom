package dev.ktpm.repository;

import dev.ktpm.model.User;

import java.util.Optional;
import java.util.UUID;

public interface IUserRepository {
	Optional<User> get(UUID userUUID);

	Optional<User> getByUsername(String username);

	User create(User user);

	User update(User user);

	void delete(UUID userUUID);
}
