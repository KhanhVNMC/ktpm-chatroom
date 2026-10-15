package dev.ktpm.impl.jpa.repos;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.ktpm.impl.jpa.HibernateRuntime;
import dev.ktpm.impl.jpa.entities.RoomMembershipEntity;
import dev.ktpm.impl.jpa.entities.RoomMembershipId;
import dev.ktpm.model.RoomMembership;
import dev.ktpm.repository.IRoomMembershipRepository;

public class JPARoomMembershipRepository implements IRoomMembershipRepository {
	private final HibernateRuntime runtime;
	
	public JPARoomMembershipRepository(HibernateRuntime runtime) {
		this.runtime = runtime;
	}
	
	@Override
	public Optional<RoomMembership> get(UUID roomUUID, UUID userUUID) {
		try (var man = runtime.openEntityManager()) {
			RoomMembershipEntity rme = man.find(
				RoomMembershipEntity.class, new RoomMembershipId(roomUUID, userUUID)
			);
			return rme == null ? Optional.empty() : Optional.of(rme.toModel());
		}
	}
	
	@Override
	public boolean exists(UUID roomUUID, UUID userUUID) {
		try (var man = runtime.open()) {
			return man.eman.createQuery("""
				SELECT COUNT(m) FROM RoomMembershipEntity m
				WHERE m.id.roomUUID = :room_uuid AND m.id.userUUID = :user_uuid
			""", Long.class)
			.setParameter("room_uuid", roomUUID)
			.setParameter("user_uuid", userUUID)
			.getSingleResult() > 0; // count
		}
	}
	
	@Override
	public List<RoomMembership> getMemberships(UUID roomUUID) {
		// could be useful if a gui... exists?
		try (var man = runtime.open()) {
			return man.eman.createQuery("""
				SELECT m FROM RoomMembershipEntity m
				WHERE m.id.roomUUID = :room_uuid
				ORDER BY m.joinedAt, m.id.userUUID
			""", RoomMembershipEntity.class)
			.setParameter("room_uuid", roomUUID)
			.getResultList().stream()
			.map(RoomMembershipEntity::toModel).toList();
		}
	}
	
	@Override
	public List<UUID> getMemberUUIDs(UUID roomUUID) {
		// mostly for listing members
		try (var man = runtime.open()) {
			return man.eman.createQuery("""
					SELECT m.id.userUUID FROM RoomMembershipEntity m
					WHERE m.id.roomUUID = :room_uuid AND m.isInRoom = true
				ORDER BY m.joinedAt, m.id.userUUID
			""", UUID.class)
			.setParameter("room_uuid", roomUUID)
			.getResultList();
		}
	}

	@Override
	public int countMembers(UUID roomUUID) {
		try (var man = runtime.open()) {
			return man.eman.createQuery("""
				SELECT COUNT(m) FROM RoomMembershipEntity m
				WHERE m.id.roomUUID = :room_uuid AND m.isInRoom = true
			""", Long.class)
			.setParameter("room_uuid", roomUUID)
			.getSingleResult().intValue();
		}
	}
	
	@Override
	public List<UUID> getPeerUUIDs(UUID userUUID) {
		try (var man = runtime.open()) {
			return man.eman.createQuery("""
				SELECT DISTINCT p.id.userUUID FROM RoomMembershipEntity m, RoomMembershipEntity p
				WHERE m.id.userUUID = :user_uuid AND m.isInRoom = true
				AND p.id.roomUUID = m.id.roomUUID AND p.isInRoom = true
			""", UUID.class)
			.setParameter("user_uuid", userUUID)
			.getResultList();
		}
	}

	@Override
	public void create(RoomMembership membership) {
		try (var man = runtime.open()) {
			man.eman.persist(RoomMembershipEntity.fromModel(membership));
		}
	}
	
	@Override
	public void update(RoomMembership membership) {
		try (var man = runtime.open()) {
			RoomMembershipEntity existing = man.eman.find(
				RoomMembershipEntity.class,
				new RoomMembershipId(membership.roomUUID(), membership.userUUID())
			);
			if (existing == null) {
				return;
			}
			existing.setJoinedAt(membership.joinedAt());
			existing.setPermissionBits(membership.permissionBits());
			existing.setStatusBits(membership.statusBits());
			existing.setInRoom(membership.isInRoom());
		}
	}
	
	@Override
	public void delete(UUID roomUUID, UUID userUUID) {
		try (var man = runtime.open()) {
			man.eman.remove(man.eman.getReference(
				RoomMembershipEntity.class, new RoomMembershipId(roomUUID, userUUID)
			));
		}
	}

	@Override
	public void deleteAllInRoom(UUID roomUUID) {
		try (var man = runtime.open()) {
			man.eman.createQuery("""
				DELETE FROM RoomMembershipEntity m WHERE m.id.roomUUID = :room_uuid
			""")
			.setParameter("room_uuid", roomUUID)
			.executeUpdate();
		}
	}
}
