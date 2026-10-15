package dev.ktpm.impl.jpa.repos;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.ktpm.impl.jpa.HibernateRuntime;
import dev.ktpm.impl.jpa.entities.ChatRoomEntity;
import dev.ktpm.model.ChatRoom;
import dev.ktpm.repository.IChatRoomRepository;

public class JPAChatRoomRepository implements IChatRoomRepository {
	private final HibernateRuntime runtime;
	
	public JPAChatRoomRepository(HibernateRuntime runtime) {
		this.runtime = runtime;
	}
	
	@Override
	public Optional<ChatRoom> get(UUID roomUUID) {
		try (var man = runtime.openEntityManager()) {
			ChatRoomEntity cre = man.find(ChatRoomEntity.class, roomUUID);
			return cre == null ? Optional.empty() : Optional.of(cre.toModel());
		}
	}
	
	@Override
	public ChatRoom create(ChatRoom room) {
		try (var man = runtime.open()) {
			var cre = ChatRoomEntity.fromModel(room);
			man.eman.persist(cre);
			man.eman.flush();
			return cre.toModel();
		}
	}
	
	@Override
	public ChatRoom update(ChatRoom room) {
		try (var man = runtime.open()) {
			ChatRoomEntity existing = man.eman.find(ChatRoomEntity.class, room.uuid());
			if (existing == null) {
				return null;
			}
			existing.setOwnerUUID(room.owner());
			existing.setName(room.name());
			existing.setMotd(room.motd());
			existing.setAvatar(room.avatar() == null ? null : room.avatar().toString());
			existing.setPublic(room.isPublic());
			existing.setInviteCode(room.inviteCode());
			return existing.toModel();
		}
	}
	
	@Override
	public void delete(UUID roomUUID) {
		try (var man = runtime.open()) {
			man.eman.remove(man.eman.getReference(ChatRoomEntity.class, roomUUID));
		}
	}
	
	@Override
	public Optional<ChatRoom> getByInviteCode(String inviteCode) {
		try (var man = runtime.open()) {
			return man.eman.createQuery("""
				SELECT r FROM ChatRoomEntity r WHERE r.inviteCode = :invite_code
			""", ChatRoomEntity.class)
			.setParameter("invite_code", inviteCode)
			.getResultList().stream().findFirst().map(ChatRoomEntity::toModel);
		}
	}
	
	@Override
	public List<ChatRoom> listPublicRooms(int limit) {
		try (var man = runtime.open()) {
			return man.eman.createQuery("""
				SELECT r FROM ChatRoomEntity r
				WHERE r.isPublic = true
				ORDER BY r.createdAt DESC, r.uuid
			""", ChatRoomEntity.class)
			.setMaxResults(limit)
			.getResultList().stream()
			.map(ChatRoomEntity::toModel).toList();
		}
	}
	
	@Override
	public List<ChatRoom> listJoinedRooms(UUID userUUID, int limit) {
		try (var man = runtime.open()) {
			return man.eman.createQuery("""
				SELECT r FROM ChatRoomEntity r
				JOIN RoomMembershipEntity m ON m.id.roomUUID = r.uuid
				WHERE m.id.userUUID = :user_uuid AND m.isInRoom = true
				ORDER BY r.createdAt DESC, r.uuid
			""", ChatRoomEntity.class)
			.setParameter("user_uuid", userUUID)
			.setMaxResults(limit)
			.getResultList().stream()
			.map(ChatRoomEntity::toModel).toList();
		}
	}
}
