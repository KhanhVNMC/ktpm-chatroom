package dev.ktpm.impl.jpa.entities;

import dev.ktpm.model.ChatRoom;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "chat_rooms")
@Getter @Setter
@NoArgsConstructor
public class ChatRoomEntity {
	@Id
	@Column(name = "uuid")
	private UUID uuid;
	
	@Column(name = "owner_uuid", nullable = false)
	private UUID ownerUUID;
	
	@Column(name = "motd")
	private String motd;
	
	@Column(name = "is_public", nullable = false)
	private boolean isPublic;
	
	// null = ko có invite code (chỉ dùng cho phòng private)
	@Column(name = "invite_code")
	private String inviteCode;

	public static ChatRoomEntity fromModel(ChatRoom model) {
		var entity = new ChatRoomEntity();
		entity.uuid = model.uuid();
		entity.ownerUUID = model.owner();
		entity.motd = model.motd();
		entity.isPublic = model.isPublic();
		entity.inviteCode = model.inviteCode();
		return entity;
	}

	public ChatRoom toModel() {
		return new ChatRoom(uuid, ownerUUID, motd, isPublic, inviteCode);
	}
}
