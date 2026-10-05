package dev.ktpm.repository;

import dev.ktpm.model.RoomMembership;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IRoomMembershipRepository {
	Optional<RoomMembership> get(UUID roomUUID, UUID userUUID);

	boolean exists(UUID roomUUID, UUID userUUID);

	List<RoomMembership> getMembers(UUID roomUUID);

	List<UUID> getMemberUUIDs(UUID roomUUID);

	int countMembers(UUID roomUUID);

	void create(RoomMembership membership);

	void update(RoomMembership membership);

	void delete(UUID roomUUID, UUID userUUID);
}
