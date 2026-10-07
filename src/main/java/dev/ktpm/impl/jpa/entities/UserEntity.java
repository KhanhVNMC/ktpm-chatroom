package dev.ktpm.impl.jpa.entities;

import dev.ktpm.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "users")
@Getter @Setter
@NoArgsConstructor
public class UserEntity {
	@Id
	@Column(name = "uuid")
	private UUID uuid;
	
	@Column(name = "username", nullable = false, unique = true)
	private String username;
	
	@Column(name = "display_name")
	private String displayName;

	public static UserEntity fromModel(User model) {
		var entity = new UserEntity();
		entity.uuid = model.uuid();
		entity.username = model.username();
		entity.displayName = model.displayName();
		return entity;
	}

	public User toModel() {
		return new User(uuid, username, displayName);
	}
}
