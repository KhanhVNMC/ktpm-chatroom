package dev.ktpm.service;

import dev.ktpm.api.dto.UploadedFile;
import dev.ktpm.model.ChatMessage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IChatMessageService {
	List<ChatMessage> getHistory(UUID requester, UUID roomUUID, int limit, Optional<UUID> beforeMessage);
	
	// search by content, same access rules and pagination as getHistory (newest -> oldest)
	List<ChatMessage> search(UUID requester, UUID roomUUID, String query, int limit, Optional<UUID> beforeMessage);
	
	// content can be empty if there are attachments (eg. voice messages)
	ChatMessage sendMessage(UUID senderUUID, UUID roomUUID, String content, Optional<List<UploadedFile>> uploadedFiles);
	
	void deleteMessage(UUID requester, UUID roomUUID, UUID messageUUID);
	
	ChatMessage editMessage(UUID requester, UUID roomUUID, UUID messageUUID, String newContent, boolean keepAttachments);
}
