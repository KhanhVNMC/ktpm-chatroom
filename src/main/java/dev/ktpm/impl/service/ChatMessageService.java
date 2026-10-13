package dev.ktpm.impl.service;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.ktpm.consts.RoomMemberStatus;
import dev.ktpm.consts.RoomPermissions;
import dev.ktpm.model.AttachmentAsset;
import dev.ktpm.model.ChatMessage;
import dev.ktpm.model.ChatRoom;
import dev.ktpm.model.RoomMembership;
import dev.ktpm.repository.IChatMessageRepository;
import dev.ktpm.repository.IChatRoomRepository;
import dev.ktpm.repository.IRoomMembershipRepository;
import dev.ktpm.service.IChatMessageService;

public class ChatMessageService implements IChatMessageService {
    private final IChatMessageRepository chatMessageRepo;
    private final IRoomMembershipRepository roomMembershipRepo;
    private final IChatRoomRepository chatRoomRepo;

    public ChatMessageService (
        IChatMessageRepository chatMessageRepo, 
        IRoomMembershipRepository roomMembershipRepo, 
        IChatRoomRepository chatRoomRepo 
    ) {
            this.chatMessageRepo = chatMessageRepo;
            this.roomMembershipRepo = roomMembershipRepo;
            this.chatRoomRepo = chatRoomRepo;
        }

    /**
     * Tải lịch sử chat.
     * 
     * Giới hạn pagesize tối đa là 100, nếu limit <= 0 thì mặc định là 50.
     * Cho phép truy cập nếu ko bị cấm, hoặc đây là phòng công khai.
     */
    @Override
    public List<ChatMessage> getHistory(UUID requester, UUID roomUUID, int limit, Optional<UUID> beforeMessage) {
        if (roomUUID == null) {
            return List.of();
        }

		var membership = requester == null ? null : roomMembershipRepo.get(roomUUID, requester).orElse(null);
		if (membership != null) {
			if ((membership.statusBits() & RoomMemberStatus.BANNED) != 0) {
				throw new IllegalStateException("You are banned from this room");
			}
		} else if (!chatRoomRepo.get(roomUUID).map(ChatRoom::isPublic).orElse(false)) {
			// ko phải member (hoặc chưa đăng nhập) thì chỉ xem được phòng public
			throw new IllegalStateException("You are not a member of this room");
		}

        // Khống chế limit và beforeMessage tránh hết bộ nhớ 
        int maxLimit = (limit <= 0) ? 50 : Math.min(limit, 100);
        UUID beforeMessageUUID = (beforeMessage != null && beforeMessage.isPresent()) ? beforeMessage.get() : null;
        return chatMessageRepo.getHistory(roomUUID, beforeMessageUUID, maxLimit);
    }

    /**
     * GỬi tin nhắn.
     * 
     * Người gửi phải tham gia phòng, ko bị cấm và có quyền gửi tin nhắn.
     */
    @Override
    public ChatMessage sendMessage(UUID senderUUID, UUID roomUUID, String content,
            Optional<List<AttachmentAsset>> uploadedFiles) {
        if (senderUUID == null || roomUUID == null) {
            throw new IllegalArgumentException("Sender UUID and room UUID must not be null");
        }
        
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Message content must not be null or blank");
        }

        Optional<RoomMembership> membershipOpt = roomMembershipRepo.get(roomUUID, senderUUID);
        if (membershipOpt.isEmpty()) {
            throw new IllegalStateException("User is not a member of this room");
        
        }

        RoomMembership membership = membershipOpt.get();
        if ((membership.statusBits() & (RoomMemberStatus.MUTED | RoomMemberStatus.BANNED | RoomMemberStatus.SUSPENDED)) != 0) {
            throw new IllegalStateException("User is muted, banned, or suspended from this room");
        }

        if ((membership.permissionBits() & RoomPermissions.SEND_MESSAGES) == 0) {
            throw new IllegalStateException("User do not have permission to send messages in this room");
        }

        List<URI> attachments = new java.util.ArrayList<>();
        if (uploadedFiles != null && uploadedFiles.isPresent()) {
            List<AttachmentAsset> assets = uploadedFiles.get();
            if (assets != null) {
                for (AttachmentAsset asset : assets) {
                    attachments.add(URI.create("/api/assets/" + asset.uuid()));
                }
            }
        }

        ChatMessage newMessage = new ChatMessage(
            UUID.randomUUID(),
            System.currentTimeMillis(),
            roomUUID,
            senderUUID,
            content.trim(), // Cắt khoảng trắng thừa ở đầu và cuối
            attachments
        );

        return chatMessageRepo.create(newMessage);
    }

    /**
     * Xóa tin nhắn.
     *
     * Người gửi hoặc người có quyền xóa tin nhắn mới được xóa.
     */
    @Override
    public void deleteMessage(UUID requester, UUID roomUUID, UUID messageUUID) {
        if (requester == null || roomUUID == null || messageUUID == null) {
            throw new IllegalArgumentException("Requester UUID, room UUID, and message UUID must not be null");
        }

        Optional<ChatMessage> messageOpt = chatMessageRepo.get(messageUUID);
        if (messageOpt.isEmpty()) {
            return; 
        }

        ChatMessage message = messageOpt.get();
        if (!message.chatRoomUUID().equals(roomUUID)) {
            throw new IllegalArgumentException("Message does not belong to the specified room");
        }

        Optional<RoomMembership> membershipOpt = roomMembershipRepo.get(roomUUID, requester);
        if (membershipOpt.isEmpty()) {
            throw new IllegalStateException("User is not a member of this room");
        }

       RoomMembership membership = membershipOpt.get();
        if (!message.sender().equals(requester) && (membership.permissionBits() & RoomPermissions.DELETE_MESSAGES) == 0) {
            throw new IllegalStateException("User do not have permission to delete this message");
        }

        chatMessageRepo.delete(messageUUID);
    }

    /**
     * Chỉnh sửa tin nhắn.
     *
     * Người gửi hoặc người có quyền chỉnh sửa tin nhắn mới được chỉnh sửa.
     */
    @Override
    public ChatMessage editMessage(UUID requester, UUID roomUUID, UUID messageUUID, String newContent,
            boolean keepAttachments) {
        if (requester == null || roomUUID == null || messageUUID == null) {
            throw new IllegalArgumentException("Requester UUID, room UUID, and message UUID must not be null");
        }
        
        if (newContent == null || newContent.isBlank()) {
            throw new IllegalArgumentException("New content must not be null or blank");
        }

        Optional<ChatMessage> messageOpt = chatMessageRepo.get(messageUUID);
        if (messageOpt.isEmpty()) {
            throw new IllegalArgumentException("Message not found");
        }

        ChatMessage existingMessage = messageOpt.get();
        if (!existingMessage.chatRoomUUID().equals(roomUUID)) {
            throw new IllegalArgumentException("Message does not belong to the specified room");
        }

        Optional<RoomMembership> membershipOpt = roomMembershipRepo.get(roomUUID, requester);
        if (membershipOpt.isEmpty()) {
            throw new IllegalStateException("User is not a member of this room");
        }

        RoomMembership membership = membershipOpt.get();
        if (!existingMessage.sender().equals(requester) && (membership.permissionBits() & RoomPermissions.EDIT_MESSAGES) == 0) {
            throw new IllegalStateException("User do not have permission to edit this message");
        }

        List<URI> attachments = keepAttachments ? existingMessage.attachments() : List.of();

        ChatMessage updatedMessage = new ChatMessage(
            existingMessage.chatMessageUUID(),
            existingMessage.sentTimestamp(),
            existingMessage.chatRoomUUID(),
            existingMessage.sender(),
            newContent.trim(),
            attachments
        );

        return chatMessageRepo.update(updatedMessage);
    }
}