package dev.ktpm.impl.jpa;

import dev.ktpm.model.Session;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "sessions")
@Getter @Setter
@NoArgsConstructor
public class SessionEntity {
	@Id
	@Column(name = "session_id")
	private String sessionID;
	
	@Column(name = "user_uuid", nullable = false)
	private UUID userUUID;
	
	@Column(name = "created_at", nullable = false)
	private long createdAt;
	
	@Column(name = "expires_at", nullable = false)
	private long expiresAt;
	
	public static SessionEntity fromModel(Session model) {
		var entity = new SessionEntity();
		entity.sessionID = model.sessionID();
		entity.userUUID = model.userUUID();
		entity.createdAt = model.createdAt();
		entity.expiresAt = model.expiresAt();
		return entity;
	}
	
	public Session toModel() {
		return new Session(sessionID, userUUID, createdAt, expiresAt);
	}
}
