package dev.ktpm.impl.service;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.ktpm.api.dto.UploadedFile;
import dev.ktpm.consts.RoomMemberPunishStatus;
import dev.ktpm.consts.RoomPermissions;
import dev.ktpm.model.ChatMessage;
import dev.ktpm.model.ChatRoom;
import dev.ktpm.model.RoomMembership;
import dev.ktpm.repository.IChatMessageRepository;
import dev.ktpm.repository.IChatRoomRepository;
import dev.ktpm.repository.IRoomMembershipRepository;
import dev.ktpm.service.IAssetService;
import dev.ktpm.service.IChatMessageService;

public class ChatMessageService implements IChatMessageService {
	private static final int MAX_CONTENT_LENGTH = 4000;
	private static final int MAX_ATTACHMENTS = 5; // we aint elon musk
	
	private final IChatMessageRepository chatMessageRepo;
	private final IRoomMembershipRepository roomMembershipRepo;
	private final IChatRoomRepository chatRoomRepo;
	private final IAssetService assetService;
	
	public ChatMessageService (
		IChatMessageRepository chatMessageRepo,
		IRoomMembershipRepository roomMembershipRepo,
		IChatRoomRepository chatRoomRepo,
		IAssetService assetService
	) {
		this.chatMessageRepo = chatMessageRepo;
		this.roomMembershipRepo = roomMembershipRepo;
		this.chatRoomRepo = chatRoomRepo;
		this.assetService = assetService;
	}
	
	@Override
	public List<ChatMessage> getHistory(UUID requester, UUID roomUUID, int limit, Optional<UUID> beforeMessage) {
		if (roomUUID == null) {
			return List.of();
		}
		this.requireReadAccess(requester, roomUUID);
		return chatMessageRepo.getHistory(roomUUID, beforeMessage.orElse(null), normalizeLimit(limit));
	}
	
	@Override
	public List<ChatMessage> search(UUID requester, UUID roomUUID, String query, int limit, Optional<UUID> beforeMessage) {
		if (roomUUID == null) {
			return List.of();
		}
		if (query == null || query.isBlank()) {
			throw new IllegalArgumentException("Search query must not be blank");
		}
		this.requireReadAccess(requester, roomUUID);
		return chatMessageRepo.search(roomUUID, query.trim(), beforeMessage.orElse(null), normalizeLimit(limit));
	}
	
	@Override
	public ChatMessage sendMessage(
		UUID senderUUID, UUID roomUUID, 
		String content, Optional<List<UploadedFile>> uploadedFiles
	) {
		if (senderUUID == null || roomUUID == null) {
			throw new IllegalArgumentException("Sender UUID and room UUID must not be null");
		}
		List<UploadedFile> files = uploadedFiles == null ? List.of() : uploadedFiles.orElse(List.of());
		if ((content == null || content.isBlank()) && files.isEmpty()) {
			throw new IllegalArgumentException("Message content must not be null or blank");
		}
		if (content != null && content.length() > MAX_CONTENT_LENGTH) {
			throw new IllegalArgumentException("Message content is too long (max " + MAX_CONTENT_LENGTH + " characters)");
		}
		if (files.size() > MAX_ATTACHMENTS) {
			throw new IllegalArgumentException("Too many attachments (max " + MAX_ATTACHMENTS + ")");
		}
		chatRoomRepo.get(roomUUID).orElseThrow(() -> new IllegalArgumentException("Room not found/doesn't exist"));
		
		RoomMembership membership = roomMembershipRepo.get(roomUUID, senderUUID)
			.filter(RoomMembership::isInRoom)
			.orElseThrow(() -> new IllegalStateException("User is not a member of this room"))
		;
		if (membership.has(RoomMemberPunishStatus.MUTED)) {
			throw new IllegalStateException("User is muted in this room");
		}
		if (!membership.can(RoomPermissions.SEND_MESSAGES)) {
			throw new IllegalStateException("User do not have permission to send messages in this room");
		}
		
		// files are uploaded before the message is created
		// if sth blows up clean up what was uploaded
		List<URI> attachments = new ArrayList<>();
		try {
			for (UploadedFile file : files) {
				attachments.add(IAssetService.uri(assetService.upload(file.data(), file.originalName()).id()));
			}
			return chatMessageRepo.create(new ChatMessage(
				UUID.randomUUID(), System.currentTimeMillis(), roomUUID, senderUUID,
				content == null ? "" : content.trim(),
				attachments
			));
		} catch (RuntimeException e) {
			assetService.deleteAll(attachments);
			throw e;
		}
	}
	
	@Override
	public void deleteMessage(UUID requester, UUID roomUUID, UUID messageUUID) {
		if (requester == null || roomUUID == null || messageUUID == null) {
			throw new IllegalArgumentException("Requester UUID, room UUID, and message UUID must not be null");
		}
		
		ChatMessage message = chatMessageRepo.get(messageUUID)
			.orElseThrow(() -> new IllegalArgumentException("Message not found"))
		;
		if (!message.chatRoomUUID().equals(roomUUID)) {
			throw new IllegalArgumentException("Message does not belong to the specified room");
		}
		
		RoomMembership membership = this.requireInRoom(roomUUID, requester);
		if (!message.sender().equals(requester) && !membership.can(RoomPermissions.DELETE_MESSAGES)) {
			throw new IllegalStateException("User do not have permission to delete this message");
		}
		
		chatMessageRepo.delete(messageUUID);
		assetService.deleteAll(message.attachments());
	}
	
	@Override
	public ChatMessage editMessage(UUID requester, UUID roomUUID, UUID messageUUID, String newContent,
			boolean keepAttachments) {
		if (requester == null || roomUUID == null || messageUUID == null) {
			throw new IllegalArgumentException("Requester UUID, room UUID, and message UUID must not be null");
		}
		
		ChatMessage existingMessage = chatMessageRepo.get(messageUUID)
			.orElseThrow(() -> new IllegalArgumentException("Message not found"))
		;
		if (!existingMessage.chatRoomUUID().equals(roomUUID)) {
			throw new IllegalArgumentException("Message does not belong to the specified room");
		}
		
		// content may be empty if the attachments are kept
		List<URI> attachments = keepAttachments ? existingMessage.attachments() : List.of();
		if ((newContent == null || newContent.isBlank()) && attachments.isEmpty()) {
			throw new IllegalArgumentException("New content must not be null or blank");
		}
		if (newContent != null && newContent.length() > MAX_CONTENT_LENGTH) {
			throw new IllegalArgumentException("Message content is too long (max " + MAX_CONTENT_LENGTH + " characters)");
		}
		
		RoomMembership membership = requireInRoom(roomUUID, requester);
		if (membership.has(RoomMemberPunishStatus.MUTED)) {
			throw new IllegalStateException("User is muted in this room");
		}
		// owner of the message AND has the edit permission, nobody gets to 
		// shove their dih to one's mouth
		if (!existingMessage.sender().equals(requester)) {
			throw new IllegalStateException("You can only edit your own messages");
		}
		if (!membership.can(RoomPermissions.EDIT_MESSAGES)) {
			throw new IllegalStateException("User do not have permission to edit messages");
		}
		
		ChatMessage updatedMessage = chatMessageRepo.update(new ChatMessage(
			existingMessage.chatMessageUUID(),
			existingMessage.sentTimestamp(),
			existingMessage.chatRoomUUID(),
			existingMessage.sender(),
			newContent == null ? "" : newContent.trim(),
			attachments
		));
		if (!keepAttachments) {
			assetService.deleteAll(existingMessage.attachments());
		}
		return updatedMessage;
	}
	
	private RoomMembership requireInRoom(UUID roomUUID, UUID userUUID) {
		return roomMembershipRepo.get(roomUUID, userUUID).filter(RoomMembership::isInRoom)
			.orElseThrow(() -> new IllegalStateException("User is not a member of this room"))
		;
	}
	
	// non members (or not logged in) can only see public rooms, and not if banned
	private void requireReadAccess(UUID requester, UUID roomUUID) {
		ChatRoom room = chatRoomRepo.get(roomUUID).orElseThrow(() -> new IllegalArgumentException("Room not found"));
		RoomMembership membership = requester == null ? null : roomMembershipRepo.get(roomUUID, requester).orElse(null);
		if (membership != null && membership.has(RoomMemberPunishStatus.BANNED)) {
			throw new IllegalStateException("You are banned from this room");
		}
		if ((membership == null || !membership.isInRoom()) && !room.isPublic()) {
			throw new IllegalStateException("You are not a member of this room");
		}
	}
	
	// Khống chế limit tránh hết bộ nhớ
	private static int normalizeLimit(int limit) {
		return (limit <= 0) ? 50 : Math.min(limit, 100);
	}
}
