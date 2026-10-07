package dev.ktpm.impl.jpa;

import dev.ktpm.model.RoomMembership;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "room_memberships")
@Getter @Setter
@NoArgsConstructor
public class RoomMembershipEntity {
	@EmbeddedId
	private RoomMembershipId id;
	
	@Column(name = "joined_at", nullable = false)
	private long joinedAt;
	
	@Column(name = "permission_bits", nullable = false)
	private int permissionBits;
	
	@Column(name = "status_bits", nullable = false)
	private int statusBits;

	public static RoomMembershipEntity fromModel(RoomMembership model) {
		var entity = new RoomMembershipEntity();
		entity.id = new RoomMembershipId(model.roomUUID(), model.userUUID());
		entity.joinedAt = model.joinedAt();
		entity.permissionBits = model.permissionBits();
		entity.statusBits = model.statusBits();
		return entity;
	}

	public RoomMembership toModel() {
		return new RoomMembership(id.getRoomUUID(), id.getUserUUID(), joinedAt, permissionBits, statusBits);
	}
}
