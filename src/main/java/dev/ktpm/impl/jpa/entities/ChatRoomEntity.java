package dev.ktpm.impl.jpa.entities;

import dev.ktpm.model.ChatRoom;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnDefault;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.nio.file.Path;
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
	
	@Column(name = "name")
	private String name;
	
	@Column(name = "motd", length = 512)
	private String motd;
	
	// path of the avatar file (serveravatars/<room uuid>.<ext>), null = none
	@Column(name = "avatar_path")
	private String avatar;
	
	@ColumnDefault("0")
	@Column(name = "created_at", nullable = false)
	private long createdAt;
	
	@Column(name = "is_public", nullable = false)
	private boolean isPublic;
	
	// null = ko có invite code (cái này chỉ dùng cho phòng private)
	@Column(name = "invite_code", unique = true)
	private String inviteCode;
	
	public static ChatRoomEntity fromModel(ChatRoom model) {
		var entity = new ChatRoomEntity();
		entity.uuid = model.uuid();
		entity.ownerUUID = model.owner();
		entity.name = model.name();
		entity.motd = model.motd();
		entity.avatar = model.avatar() == null ? null : model.avatar().toString();
		entity.createdAt = model.createdAt();
		entity.isPublic = model.isPublic();
		entity.inviteCode = model.inviteCode();
		return entity;
	}
	
	public ChatRoom toModel() {
		return new ChatRoom(uuid, ownerUUID, name, motd, avatar == null ? null : Path.of(avatar), createdAt, isPublic, inviteCode);
	}
}
