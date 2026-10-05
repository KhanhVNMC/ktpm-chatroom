package dev.ktpm.repository;

import dev.ktpm.model.UserCredentials;

import java.util.Optional;
import java.util.UUID;

public interface IUserCredentialsRepository {
	Optional<UserCredentials> get(UUID userUUID);

	Optional<UserCredentials> getByUsername(String username);

	void set(UserCredentials credentials);
}
