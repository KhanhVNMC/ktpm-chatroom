package dev.ktpm.impl.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.ktpm.api.dto.RoomOverviewResponse;
import dev.ktpm.consts.RoomMemberStatus;
import dev.ktpm.consts.RoomPermissions;
import dev.ktpm.model.ChatRoom;
import dev.ktpm.model.RoomMembership;
import dev.ktpm.model.User;
import dev.ktpm.repository.IChatMessageRepository;
import dev.ktpm.repository.IChatRoomRepository;
import dev.ktpm.repository.IRoomMembershipRepository;
import dev.ktpm.repository.IUserRepository;
import dev.ktpm.service.IAssetService;
import dev.ktpm.service.IChatRoomService;

public class ChatRoomService implements IChatRoomService {
	private static final String INVITE_CODE_PATTERN = "[A-Za-z0-9_-]{4,32}";

	private final IChatRoomRepository chatRoomRepo;
	private final IRoomMembershipRepository roomMembershipRepo;
	private final IUserRepository userRepo;
	private final IChatMessageRepository chatMessageRepo;
	private final IAssetService assetService;

	public ChatRoomService(
		IChatRoomRepository chatRoomRepo,
		IRoomMembershipRepository roomMembershipRepo,
		IUserRepository userRepo,
		IChatMessageRepository chatMessageRepo,
		IAssetService assetService
	) {
		this.chatRoomRepo = chatRoomRepo;
		this.roomMembershipRepo = roomMembershipRepo;
		this.userRepo = userRepo;
		this.chatMessageRepo = chatMessageRepo;
		this.assetService = assetService;
	}

	@Override
	public ChatRoom getRoom(UUID roomUUID) {
		if (roomUUID == null) {
			throw new IllegalArgumentException("Room UUID must not be null");
		}
		return chatRoomRepo.get(roomUUID).orElseThrow(() -> new IllegalArgumentException("Room not found"));
	}

	@Override
	public List<ChatRoom> listPublicRooms(UUID requester, int limit) {
		if (requester != null && userRepo.get(requester).isEmpty()) {
			throw new IllegalArgumentException("Requester not found");
		}
		return chatRoomRepo.listPublicRooms(normalizeLimit(limit));
	}

	@Override
	public List<ChatRoom> listJoinedRooms(UUID requester, int limit) {
		if (requester == null || userRepo.get(requester).isEmpty()) {
			throw new IllegalArgumentException("Requester not found");
		}
		return chatRoomRepo.listJoinedRooms(requester, normalizeLimit(limit));
	}

	@Override
	public int getMemberCount(UUID roomUUID) {
		return roomMembershipRepo.countMembers(roomUUID);
	}

	@Override
	public ChatRoom createRoom(UUID owner, String name, String motd, boolean isPublic, Optional<String> inviteCode) {
		if (owner == null || userRepo.get(owner).isEmpty()) {
			throw new IllegalArgumentException("Owner not found");
		}
		String roomName = requireName(name);
		String roomMotd = requireMotd(motd);

		// public rooms dont need a code
		String code = null;
		if (!isPublic) {
			code = inviteCode.filter(c -> !c.isBlank()).map(String::trim).orElseGet(this::generateInviteCode);
			requireFreeInviteCode(code);
		}
		ChatRoom room = chatRoomRepo.create(new ChatRoom(
			UUID.randomUUID(), owner, roomName, roomMotd, null, System.currentTimeMillis(), isPublic, code
		));

		// owner gets all perms
		roomMembershipRepo.create(new RoomMembership(
			room.uuid(), owner, room.createdAt(), RoomPermissions.ALL, 0, true
		));
		return room;
	}

	@Override
	public void joinRoom(UUID userUUID, UUID roomUUID, Optional<String> inviteCode) {
		ChatRoom room = getRoom(roomUUID);
		// private room, needs the right invite code
		if (!room.isPublic() && (room.inviteCode() == null || !room.inviteCode().equals(inviteCode.orElse(null)))) {
			throw new IllegalStateException("This is a private room, you need a valid invite code to join");
		}
		join(userUUID, room);
	}

	@Override
	public ChatRoom joinByInviteCode(UUID userUUID, String inviteCode) {
		ChatRoom room = findByInviteCode(inviteCode);
		join(userUUID, room);
		return room;
	}

	@Override
	public void leaveRoom(UUID userUUID, UUID roomUUID, boolean silentLeave, Optional<String> reason) {
		if (userUUID == null || roomUUID == null) {
			throw new IllegalArgumentException("User UUID and roomUUID are null");
		}
		if (userRepo.get(userUUID).isEmpty()) {
			throw new IllegalArgumentException("User not found");
		}
		ChatRoom room = getRoom(roomUUID);

		// banned people still have a relation but isInRoom = false, so they count as not in the room
		if (!isInRoom(roomUUID, userUUID)) {
			throw new IllegalStateException("User is not a member of this room");
		}

		// owner has to transfer ownership or delete the room
		if (room.owner().equals(userUUID)) {
			throw new IllegalStateException("Room owner cannot leave the room");
		}

		roomMembershipRepo.delete(roomUUID, userUUID);

		/*
		 * silentLeave và reason chưa có luồng logic trong persistence layer,
		 * nên chưa thêm vô.
		 */
	}

	@Override
	public List<UUID> deleteRoom(UUID requesterUUID, UUID roomUUID) {
		if (requesterUUID == null || roomUUID == null) {
			throw new IllegalArgumentException("Requester UUID and room UUID must not be null");
		}
		if (userRepo.get(requesterUUID).isEmpty()) {
			throw new IllegalArgumentException("Requester not found");
		}
		ChatRoom room = getRoom(roomUUID);
		if (!room.owner().equals(requesterUUID)) {
			throw new IllegalStateException("Only the room owner can delete the room");
		}

		List<UUID> members = roomMembershipRepo.getMemberUUIDs(roomUUID);

		// clean up child -> parent: messages (+ attachments) -> memberships (banned ones too) -> room
		assetService.deleteAll(chatMessageRepo.deleteAllInRoom(roomUUID));
		roomMembershipRepo.deleteAllInRoom(roomUUID);
		chatRoomRepo.delete(roomUUID);
		assetService.deleteRoomAvatar(room.avatar());
		return members;
	}

	@Override
	public RoomOverviewResponse getOverview(UUID requester, UUID roomUUID) {
		if (roomUUID == null) {
			throw new IllegalArgumentException("Room UUID must not be null");
		}
		if (requester != null && userRepo.get(requester).isEmpty()) {
			throw new IllegalArgumentException("Requester not found");
		}
		ChatRoom room = getRoom(roomUUID);

		Optional<RoomMembership> membership = requester == null
			? Optional.empty() : roomMembershipRepo.get(roomUUID, requester).filter(RoomMembership::isInRoom);
		if (!room.isPublic() && membership.isEmpty()) {
			throw new IllegalStateException("You are not a member of this private room");
		}
		boolean seeInvite = membership.map(m -> m.can(RoomPermissions.MANAGE_ROOM)).orElse(false);
		return buildOverview(room, true, seeInvite);
	}

	@Override
	public RoomOverviewResponse getOverviewByInviteCode(String inviteCode) {
		return buildOverview(findByInviteCode(inviteCode), false, false);
	}

	@Override
	public ChatRoom updateRoom(UUID actor, UUID roomUUID, Optional<String> name, Optional<String> motd) {
		ChatRoom room = requireManageable(actor, roomUUID);
		return chatRoomRepo.update(new ChatRoom(
			room.uuid(), room.owner(),
			name.isPresent() ? requireName(name.get()) : room.name(),
			motd.isPresent() ? requireMotd(motd.get()) : room.motd(),
			room.avatar(), room.createdAt(), room.isPublic(), room.inviteCode()
		));
	}

	@Override
	public ChatRoom setAvatar(UUID actor, UUID roomUUID, byte[] data, String originalName) {
		ChatRoom room = requireManageable(actor, roomUUID);
		var avatar = assetService.saveRoomAvatar(roomUUID, data, originalName);
		return chatRoomRepo.update(new ChatRoom(
			room.uuid(), room.owner(), room.name(), room.motd(),
			avatar, room.createdAt(), room.isPublic(), room.inviteCode()
		));
	}

	@Override
	public ChatRoom removeAvatar(UUID actor, UUID roomUUID) {
		ChatRoom room = requireManageable(actor, roomUUID);
		assetService.deleteRoomAvatar(room.avatar());
		return chatRoomRepo.update(new ChatRoom(
			room.uuid(), room.owner(), room.name(), room.motd(),
			null, room.createdAt(), room.isPublic(), room.inviteCode()
		));
	}

	@Override
	public ChatRoom resetInviteCode(UUID actor, UUID roomUUID, Optional<String> inviteCode) {
		ChatRoom room = requireManageable(actor, roomUUID);
		if (room.isPublic()) {
			throw new IllegalArgumentException("Public rooms do not use invite codes");
		}
		String code = inviteCode.filter(c -> !c.isBlank()).map(String::trim).orElseGet(this::generateInviteCode);
		requireFreeInviteCode(code);
		return chatRoomRepo.update(new ChatRoom(
			room.uuid(), room.owner(), room.name(), room.motd(),
			room.avatar(), room.createdAt(), room.isPublic(), code
		));
	}

	// a ban keeps the relation so they cant rejoin to dodge it, they need an unban (relation gets deleted) before joining again
	private void join(UUID userUUID, ChatRoom room) {
		if (userUUID == null) {
			throw new IllegalArgumentException("User UUID is null");
		}
		if (userRepo.get(userUUID).isEmpty()) {
			throw new IllegalArgumentException("User not found");
		}
		Optional<RoomMembership> existing = roomMembershipRepo.get(room.uuid(), userUUID);
		if (existing.isPresent()) {
			if (existing.get().has(RoomMemberStatus.BANNED)) {
				throw new IllegalStateException("You are banned from this room");
			}
			if (existing.get().isInRoom()) {
				throw new IllegalStateException("User is already inside the room");
			}
			roomMembershipRepo.update(existing.get().withInRoom(true));
			return;
		}
		roomMembershipRepo.create(new RoomMembership(
			room.uuid(), userUUID, System.currentTimeMillis(), 
			RoomPermissions.DEFAULT_MEMBER, 0, true
		));
	}

	private ChatRoom requireManageable(UUID actor, UUID roomUUID) {
		if (actor == null) {
			throw new IllegalArgumentException("Actor UUID must not be null");
		}
		ChatRoom room = getRoom(roomUUID);
		RoomMembership membership = roomMembershipRepo.get(roomUUID, actor).filter(RoomMembership::isInRoom)
			.orElseThrow(() -> new IllegalStateException("User is not a member of this room"));
		if (!membership.can(RoomPermissions.MANAGE_ROOM)) {
			throw new IllegalStateException("You do not have permission to manage this room");
		}
		return room;
	}

	private ChatRoom findByInviteCode(String inviteCode) {
		if (inviteCode == null || inviteCode.isBlank()) {
			throw new IllegalArgumentException("Invite code is required");
		}
		return chatRoomRepo.getByInviteCode(inviteCode.trim())
			.orElseThrow(() -> new IllegalArgumentException("Invite code not found"));
	}

	private boolean isInRoom(UUID roomUUID, UUID userUUID) {
		return roomMembershipRepo.get(roomUUID, userUUID).map(RoomMembership::isInRoom).orElse(false);
	}

	private RoomOverviewResponse buildOverview(ChatRoom room, boolean withMembers, boolean withInviteCode) {
		User owner = userRepo.get(room.owner())
			.orElseThrow(() -> new IllegalStateException("Room owner not found"));

		List<User> members = !withMembers ? List.of() : roomMembershipRepo
			.getMemberUUIDs(room.uuid()).stream()
			.limit(50) // 50 members max, use the list members API for the rest
			.map(memberUUID -> userRepo.get(memberUUID))
			.flatMap(Optional::stream)
			.toList();

		return new RoomOverviewResponse(
			room.uuid(), room.name(), room.motd(), room.avatar(), room.createdAt(), room.isPublic(),
			owner.displayName() != null ? owner.displayName() : owner.username(), owner.uuid(),
			roomMembershipRepo.countMembers(room.uuid()), members,
			withInviteCode ? room.inviteCode() : null
		);
	}

	private static int normalizeLimit(int limit) {
		return limit <= 0 ? 50 : Math.min(limit, 100);
	}

	private static String requireName(String name) {
		if (name == null || name.isBlank() || name.trim().length() > 64) {
			throw new IllegalArgumentException("Room name must be 1-64 characters");
		}
		return name.trim();
	}

	private static String requireMotd(String motd) {
		if (motd == null) {
			return "";
		}
		if (motd.length() > 512) {
			throw new IllegalArgumentException("MOTD must be at most 512 characters");
		}
		return motd.trim();
	}

	private String generateInviteCode() {
		String code;
		do {
			code = UUID.randomUUID().toString().substring(0, 8);
		} while (chatRoomRepo.getByInviteCode(code).isPresent());
		return code;
	}

	private void requireFreeInviteCode(String code) {
		if (!code.matches(INVITE_CODE_PATTERN)) {
			throw new IllegalArgumentException("Invite code must be 4-32 characters (letters, digits, - or _)");
		}
		if (chatRoomRepo.getByInviteCode(code).isPresent()) {
			throw new IllegalArgumentException("Invite code is already in use");
		}
	}
}
