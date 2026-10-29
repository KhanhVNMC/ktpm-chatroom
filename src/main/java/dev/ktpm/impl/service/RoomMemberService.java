package dev.ktpm.impl.service;

import java.util.List;
import java.util.UUID;

import dev.ktpm.consts.MemberAction;
import dev.ktpm.consts.RoomMemberPunishStatus;
import dev.ktpm.consts.RoomPermissions;
import dev.ktpm.model.ChatRoom;
import dev.ktpm.model.RoomMembership;
import dev.ktpm.repository.IChatRoomRepository;
import dev.ktpm.repository.IRoomMembershipRepository;
import dev.ktpm.repository.IUserRepository;
import dev.ktpm.service.IRoomMemberService;

public class RoomMemberService implements IRoomMemberService {
	private final IChatRoomRepository chatRoomRepo;
	private final IRoomMembershipRepository roomMembershipRepo;
	private final IUserRepository userRepo;
	
	public RoomMemberService(
		IChatRoomRepository chatRoomRepo,
		IRoomMembershipRepository roomMembershipRepo,
		IUserRepository userRepo
	) {
		this.chatRoomRepo = chatRoomRepo;
		this.roomMembershipRepo = roomMembershipRepo;
		this.userRepo = userRepo;
	}
	
	@Override
	public List<RoomMembership> listMembers(UUID requester, UUID roomUUID) {
		this.getRoom(roomUUID); // throws if not found
		RoomMembership me = this.requireInRoom(roomUUID, requester);
		boolean seeBanned = me.can(RoomPermissions.BAN_MEMBERS);
		return roomMembershipRepo.getMemberships(roomUUID).stream()
			.filter(m -> m.isInRoom() || seeBanned)
		.toList();
	}

	@Override
	public void moderate(UUID actor, UUID roomUUID, UUID target, MemberAction action) {
		if (action == null) {
			throw new IllegalArgumentException("Action must not be null");
		}
		int required = switch (action) {
			case KICK -> RoomPermissions.KICK_MEMBERS;
			case BAN, UNBAN -> RoomPermissions.BAN_MEMBERS;
			case MUTE, UNMUTE -> RoomPermissions.MUTE_MEMBERS;
		};
		ChatRoom room = this.getRoom(roomUUID);
		this.authorize(room, actor, target, required);
		
		var membership = roomMembershipRepo.get(roomUUID, target);
		switch (action) {
			case BAN -> {
				if (membership.isPresent() && membership.get().has(RoomMemberPunishStatus.BANNED)) {
					throw new IllegalArgumentException("Member is already banned");
				}
				if (membership.isPresent()) {
					// keep the relation, just mark banned + left, so leaving and joining back cant dodge the ban
					roomMembershipRepo.update(membership.get()
						.withStatus(membership.get().statusBits() | RoomMemberPunishStatus.BANNED)
						.withInRoom(false)
					);
				} else {
					// banning someone who never joined is fine too
					if (userRepo.get(target).isEmpty()) {
						throw new IllegalArgumentException("User not found");
					}
					roomMembershipRepo.create(new RoomMembership(
						roomUUID, target, System.currentTimeMillis(), 0, 
						RoomMemberPunishStatus.BANNED, false
					));
				}
			}
			case UNBAN -> {
				if (membership.isEmpty() || !membership.get().has(RoomMemberPunishStatus.BANNED)) {
					throw new IllegalArgumentException("Member is not banned");
				}
				// throw the relation away, they join again like a new person
				roomMembershipRepo.delete(roomUUID, target);
			}
			case KICK -> {
				this.requireInRoom(roomUUID, target);
				roomMembershipRepo.delete(roomUUID, target); // kick = a normal leave, can still join back
			}
			case MUTE -> {
				RoomMembership m = this.requireInRoom(roomUUID, target);
				roomMembershipRepo.update(m.withStatus(m.statusBits() | RoomMemberPunishStatus.MUTED));
			}
			case UNMUTE -> {
				RoomMembership m = this.requireInRoom(roomUUID, target);
				roomMembershipRepo.update(m.withStatus(m.statusBits() & ~RoomMemberPunishStatus.MUTED));
			}
		}
	}
	
	@Override
	public void setPermissions(UUID actor, UUID roomUUID, UUID target, int permissionBits) {
		ChatRoom room = this.getRoom(roomUUID);
		RoomMembership me = this.authorize(room, actor, target, RoomPermissions.GRANT_PERMISSIONS);
		if ((permissionBits & ~RoomPermissions.ALL) != 0) {
			throw new IllegalArgumentException("Unknown permission bits");
		}
		if (!room.owner().equals(actor)) {
			// admins cant grant admin, and only the permissions they have themselves
			if ((permissionBits & RoomPermissions.GRANT_PERMISSIONS) != 0) {
				throw new IllegalStateException("Only the room owner can grant GRANT_PERMISSIONS");
			}
			if ((permissionBits & ~me.permissionBits()) != 0) { // rising edge
				throw new IllegalStateException("You cannot grant permissions you do not have");
			}
		}
		RoomMembership m = this.requireInRoom(roomUUID, target);
		roomMembershipRepo.update(m.withPermissions(permissionBits));
	}
	
	@Override
	public void transferOwnership(UUID actor, UUID roomUUID, UUID newOwner) {
		ChatRoom room = getRoom(roomUUID);
		if (actor == null || !room.owner().equals(actor)) {
			throw new IllegalStateException("Only the room owner can transfer ownership");
		}
		if (newOwner == null || newOwner.equals(actor)) {
			throw new IllegalArgumentException("New owner must be another member");
		}
		RoomMembership target = this.requireInRoom(roomUUID, newOwner);
		RoomMembership old = this.requireInRoom(roomUUID, actor);
		
		chatRoomRepo.update(new ChatRoom(
			room.uuid(), newOwner, room.name(), room.motd(),
			room.avatar(), room.createdAt(), room.isPublic(), 
			room.inviteCode()
		));
		// new owner gets everything (and gets unmuted), old owner becomes a normal member so they can actually leave
		roomMembershipRepo.update(target.withPermissions(RoomPermissions.ALL).withStatus(0));
		roomMembershipRepo.update(old.withPermissions(RoomPermissions.DEFAULT_MEMBER));
	}
	
	private ChatRoom getRoom(UUID roomUUID) {
		if (roomUUID == null) {
			throw new IllegalArgumentException("Room UUID must not be null");
		}
		return chatRoomRepo.get(roomUUID).orElseThrow(() -> new IllegalArgumentException("Room not found"));
	}

	private RoomMembership requireInRoom(UUID roomUUID, UUID userUUID) {
		if (userUUID == null) {
			throw new IllegalArgumentException("User UUID must not be null");
		}
		return roomMembershipRepo.get(roomUUID, userUUID).filter(RoomMembership::isInRoom)
			.orElseThrow(() -> new IllegalStateException("User is not a member of this room"))
		;
	}
	
	// shared checks: actor is in the room + has the permission + target is valid. returns the actor's membership
	private RoomMembership authorize(ChatRoom room, UUID actor, UUID target, int required) {
		if (target == null) {
			throw new IllegalArgumentException("Target UUID must not be null");
		}
		RoomMembership me = requireInRoom(room.uuid(), actor);
		if (!me.can(required)) {
			throw new IllegalStateException("You do not have permission to do this");
		}
		if (actor.equals(target)) {
			throw new IllegalArgumentException("You cannot touch yourself"); // pun intended
		}
		if (room.owner().equals(target)) {
			throw new IllegalStateException("You cannot do this to the room owner");
		}
		if (!room.owner().equals(actor)) {
			// admins cant touch other admins
			// aka civil war
			boolean targetIsAdmin = roomMembershipRepo.get(room.uuid(), target)
				.map(m -> m.can(RoomPermissions.GRANT_PERMISSIONS))
				.orElse(false)
			;
			if (targetIsAdmin) {
				throw new IllegalStateException("Only the room owner can do this to an admin");
			}
		}
		return me;
	}
}
