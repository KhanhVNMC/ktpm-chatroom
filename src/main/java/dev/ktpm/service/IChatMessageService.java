package dev.ktpm.service;

import dev.ktpm.model.AttachmentAsset;
import dev.ktpm.model.ChatMessage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IChatMessageService {
	List<ChatMessage> getHistory(UUID requester, UUID roomUUID, int limit, Optional<UUID> beforeMessage);
	
	ChatMessage sendMessage(UUID senderUUID, UUID roomUUID, String content, Optional<List<AttachmentAsset>> uploadedFiles);
	
	void deleteMessage(UUID requester, UUID roomUUID, UUID messageUUID);
	
	ChatMessage editMessage(UUID requester, UUID roomUUID, UUID messageUUID, String newContent, boolean keepAttachments);
}
