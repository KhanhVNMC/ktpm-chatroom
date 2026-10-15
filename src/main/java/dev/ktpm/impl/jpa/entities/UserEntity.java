package dev.ktpm.impl.jpa.entities;

import dev.ktpm.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import dev.ktpm.consts.UserPresence;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
	
	@Column(name = "avatar_asset")
	private UUID avatar;
	
	// null (users from before this column existed) = OFFLINE
	@Enumerated(EnumType.STRING)
	@Column(name = "presence")
	private UserPresence presence;
	
	@Column(name = "status_text", length = 128)
	private String statusText;

	public static UserEntity fromModel(User model) {
		var entity = new UserEntity();
		entity.uuid = model.uuid();
		entity.username = model.username();
		entity.displayName = model.displayName();
		entity.avatar = model.avatar();
		entity.presence = model.presence();
		entity.statusText = model.statusText();
		return entity;
	}

	public User toModel() {
		return new User(
			uuid, username, displayName, avatar, 
			presence == null ? UserPresence.OFFLINE : presence, statusText
		);
	}
}
