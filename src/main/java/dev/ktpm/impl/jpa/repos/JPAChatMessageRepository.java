package dev.ktpm.impl.jpa.repos;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.ktpm.impl.jpa.HibernateRuntime;
import dev.ktpm.impl.jpa.entities.ChatMessageEntity;
import dev.ktpm.model.ChatMessage;
import dev.ktpm.repository.IChatMessageRepository;

public class JPAChatMessageRepository implements IChatMessageRepository {
	private final HibernateRuntime runtime;
	
	public JPAChatMessageRepository(HibernateRuntime runtime) {
		this.runtime = runtime;
	}
	
	@Override
	public Optional<ChatMessage> get(UUID messageUUID) {
		try (var man = runtime.openEntityManager()) {
			ChatMessageEntity cme = man.find(ChatMessageEntity.class, messageUUID);
			return cme == null ? Optional.empty() : Optional.of(cme.toModel());
		}
	}
	
	@Override
	public void delete(UUID messageUUID) {
		try (var man = runtime.open()) {
			man.eman.remove(man.eman.getReference(
				ChatMessageEntity.class, messageUUID
			));
		}
	}
	
	@Override
	public ChatMessage create(ChatMessage message) {
		try (var man = runtime.open()) {
			var entity = ChatMessageEntity.fromModel(message);
			man.eman.persist(entity);
			man.eman.flush();
			return entity.toModel();
		}
	}
	
	@Override
	public ChatMessage update(ChatMessage message) {
		try (var man = runtime.open()) {
			ChatMessageEntity existing = man.eman.find(
				ChatMessageEntity.class, message.chatMessageUUID()
			);
			if (existing == null) {
				return null;
			}
			existing.setContent(message.content());
			// if null, keep existing attachments as-is
			if (message.attachments() != null) {
				existing.setAttachments(
					message.attachments().stream().map(URI::toString).toList()
				); 
			}
			return existing.toModel();
		}
	}
	
	@Override
	public List<ChatMessage> getHistory(UUID roomUUID, UUID beforeMessage, int limit) {
		try (var man = runtime.open()) {
			ChatMessageEntity before = null;
			if (beforeMessage != null) {
				before = man.eman.find(ChatMessageEntity.class, beforeMessage);
				if (before == null) {
					return List.of();
				}
			}
			// return all message that is before or at the beforeMsg sent time
			// this is not good for exact OoI (same ms) because fuck you thats why...
			// but hey, AT LEAST its deterministic
			// beforeMessage == null thì lấy từ tin mới nhất
			var query = man.eman.createQuery("""
				SELECT m FROM ChatMessageEntity m
				WHERE m.chatRoomUUID = :room_uuid
				""" + (before == null ? "" : """
				AND m.sentTimestamp <= :before_timestamp
				AND m.chatMessageUUID != :before_message
				""") + """
				ORDER BY m.sentTimestamp DESC, m.chatMessageUUID DESC
			""", ChatMessageEntity.class)
			.setParameter("room_uuid", roomUUID);
			if (before != null) {
				query.setParameter("before_timestamp", before.getSentTimestamp())
				.setParameter("before_message", beforeMessage);
			}
			return query
			.setMaxResults(limit)
			.getResultList().stream()
			.map(ChatMessageEntity::toModel).toList(); // list order is newest -> oldest
		}
	}
}
