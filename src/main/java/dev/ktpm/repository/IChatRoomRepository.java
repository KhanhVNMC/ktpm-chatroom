package dev.ktpm.repository;

import dev.ktpm.model.ChatRoom;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IChatRoomRepository {
	Optional<ChatRoom> get(UUID roomUUID);
	
	ChatRoom create(ChatRoom room);
	
	ChatRoom update(ChatRoom room);
	
	void delete(UUID roomUUID);
	
	Optional<ChatRoom> getByInviteCode(String inviteCode);
	
	List<ChatRoom> listPublicRooms(int limit);
	
	// rooms the user is currently in (isInRoom)
	List<ChatRoom> listJoinedRooms(UUID userUUID, int limit);
}
