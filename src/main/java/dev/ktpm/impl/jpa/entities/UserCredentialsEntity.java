package dev.ktpm.impl.jpa.entities;

import dev.ktpm.model.UserCredentials;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "user_credentials")
@Getter @Setter
@NoArgsConstructor
public class UserCredentialsEntity {
	@Id
	@Column(name = "user_uuid")
	private UUID userUUID;
	
	@Column(name = "password_hash")
	private String passwordHash;
	
	public static UserCredentialsEntity fromModel(UserCredentials model) {
		var entity = new UserCredentialsEntity();
		entity.userUUID = model.userUUID();
		entity.passwordHash = model.passwordHash();
		return entity;
	}
	
	public UserCredentials toModel() {
		return new UserCredentials(userUUID, passwordHash);
	}
}
