package dev.ktpm.impl.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
@Getter @Setter
@NoArgsConstructor
public class RoomMembershipId implements Serializable {
	private static final long serialVersionUID = 1L;
	
	@Column(name = "room_uuid")
	private UUID roomUUID;
	
	@Column(name = "user_uuid")
	private UUID userUUID;

	public RoomMembershipId(UUID roomUUID, UUID userUUID) {
		this.roomUUID = roomUUID;
		this.userUUID = userUUID;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) return true;
		if (!(other instanceof RoomMembershipId that)) return false;
		return Objects.equals(roomUUID, that.roomUUID) && Objects.equals(userUUID, that.userUUID);
	}

	@Override
	public int hashCode() {
		return Objects.hash(roomUUID, userUUID);
	}
}
