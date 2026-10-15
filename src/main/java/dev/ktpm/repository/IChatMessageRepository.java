package dev.ktpm.repository;

import dev.ktpm.model.ChatMessage;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IChatMessageRepository {
	Optional<ChatMessage> get(UUID messageUUID);
	
	List<ChatMessage> getHistory(UUID roomUUID, UUID beforeMessage, int limit);
	
	// search by content (case insensitive), newest -> oldest, paginated like getHistory
	List<ChatMessage> search(UUID roomUUID, String query, UUID beforeMessage, int limit);
	
	ChatMessage create(ChatMessage message);
	
	ChatMessage update(ChatMessage message);
	
	void delete(UUID messageUUID);
	
	// delete every message of the room, returns their attachments so the assets can be cleaned
	List<URI> deleteAllInRoom(UUID roomUUID);
}
