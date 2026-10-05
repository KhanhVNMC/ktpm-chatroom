package dev.ktpm.repository;

import dev.ktpm.model.ChatMessage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IChatMessageRepository {
	Optional<ChatMessage> get(UUID messageUUID);
	
	List<ChatMessage> getHistory(UUID roomUUID, UUID beforeMessage, int limit);
	
	ChatMessage create(ChatMessage message);
	
	ChatMessage update(ChatMessage message);
	
	void delete(UUID messageUUID);
}
