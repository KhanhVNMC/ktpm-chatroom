package dev.ktpm.impl.jpa;

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

import java.net.MalformedURLException;
import java.net.URI;
import java.time.Instant;
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
	
	@CurrentTimestamp(event = EventType.INSERT, source = SourceType.DB)
	@Column(name = "sent_timestamp", columnDefinition = "timestamp with time zone", nullable = false)
	@Setter(AccessLevel.NONE)
	private Instant sentTimestamp;
	
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
		entity.chatRoomUUID = model.chatRoomUUID();
		entity.senderUUID = model.sender();
		entity.content = model.content();
		entity.attachments = model.attachments().stream().map(URI::toString).toList();
		return entity;
	}

	public ChatMessage toModel() throws MalformedURLException {
		List<URI> uris = attachments.stream().map(value -> {
			return URI.create(value);
		}).toList();
		return new ChatMessage(
			this.chatMessageUUID, 
			this.sentTimestamp.toEpochMilli(),
			this.chatRoomUUID, this.senderUUID, 
			this.content, uris
		);
	}
}
