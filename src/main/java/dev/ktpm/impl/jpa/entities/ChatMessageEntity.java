package dev.ktpm.impl.jpa.entities;

import dev.ktpm.model.ChatMessage;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.CurrentTimestamp;
import org.hibernate.annotations.SourceType;
import org.hibernate.generator.EventType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import lombok.Setter;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "chat_messages")
@Getter @Setter
@NoArgsConstructor
public class ChatMessageEntity {
	@Id
	@Column(name = "chat_message_uuid")
	private UUID chatMessageUUID;
	
	@Setter(AccessLevel.NONE)
	@Column(name = "sent_timestamp", nullable = false)
	private long sentTimestamp;
	
	@Column(name = "sender_uuid", nullable = false)
	private UUID senderUUID;
	
	@Column(name = "chat_room_uuid", nullable = false)
	private UUID chatRoomUUID;
	
	@Column(name = "content")
	private String content;
	
	@ElementCollection
	@CollectionTable(name = "chat_message_attachments", joinColumns = @JoinColumn(
		name = "chat_message_uuid"
	))
	@OrderColumn(name = "attachment_order")
	@Column(name = "attachment_url")
	private List<String> attachments;
	
	public static ChatMessageEntity fromModel(ChatMessage model) {
		var entity = new ChatMessageEntity();
		entity.chatMessageUUID = model.chatMessageUUID();
		entity.sentTimestamp = model.sentTimestamp();
		entity.chatRoomUUID = model.chatRoomUUID();
		entity.senderUUID = model.sender();
		entity.content = model.content();
		entity.attachments = model.attachments().stream().map(URI::toString).toList();
		return entity;
	}
	
	public ChatMessage toModel() {
		return new ChatMessage(
			this.chatMessageUUID, this.sentTimestamp,
			this.chatRoomUUID, this.senderUUID, 
			this.content, attachments.stream().map(value -> URI.create(value)).toList()
		);
	}
}
