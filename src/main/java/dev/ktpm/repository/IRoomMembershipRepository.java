package dev.ktpm.repository;

import dev.ktpm.model.RoomMembership;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IRoomMembershipRepository {
	Optional<RoomMembership> get(UUID roomUUID, UUID userUUID);
	
	boolean exists(UUID roomUUID, UUID userUUID);
	
	// every relation of the room, banned people included (isInRoom = false)
	List<RoomMembership> getMemberships(UUID roomUUID);

	// only people currently in the room (isInRoom = true)
	List<UUID> getMemberUUIDs(UUID roomUUID);

	int countMembers(UUID roomUUID);

	// everyone sharing a room with userUUID (incl. itself), for presence
	List<UUID> getPeerUUIDs(UUID userUUID);
	
	void create(RoomMembership membership);
	
	void update(RoomMembership membership);
	
	void delete(UUID roomUUID, UUID userUUID);
	
	void deleteAllInRoom(UUID roomUUID);
}
