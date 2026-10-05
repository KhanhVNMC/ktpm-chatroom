package dev.ktpm.repository;

import dev.ktpm.model.Session;

import java.util.Optional;
import java.util.UUID;

public interface ISessionRepository {
	Session create(UUID userUUID);
	
	Optional<Session> get(String sessionID);
	
	void delete(String sessionID);
	
	void deleteAllForUser(UUID userUUID);
}
