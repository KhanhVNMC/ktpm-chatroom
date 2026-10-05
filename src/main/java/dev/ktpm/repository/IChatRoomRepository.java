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
	
	List<ChatRoom> listPublicRooms(int limit);
}
