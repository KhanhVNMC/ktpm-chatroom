package dev.ktpm.model;

import java.util.UUID;

// isInRoom = false: relation stays but counts as left (used for bans, so leave + rejoin cant dodge it)
public record RoomMembership(
	UUID roomUUID, UUID userUUID, long joinedAt, 
	int permissionBits, int statusBits, boolean isInRoom
) {
	public boolean can(int permission) {
		return (permissionBits & permission) == permission;
	}
	
	public boolean has(int status) {
		return (statusBits & status) != 0;
	}
	
	public RoomMembership withPermissions(int bits) {
		return new RoomMembership(roomUUID, userUUID, joinedAt, bits, statusBits, isInRoom);
	}
	
	public RoomMembership withStatus(int bits) {
		return new RoomMembership(roomUUID, userUUID, joinedAt, permissionBits, bits, isInRoom);
	}
	
	public RoomMembership withInRoom(boolean inRoom) {
		return new RoomMembership(roomUUID, userUUID, joinedAt, permissionBits, statusBits, inRoom);
	}
}
