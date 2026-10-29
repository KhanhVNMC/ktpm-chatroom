package dev.ktpm.service;

import dev.ktpm.api.dto.RoomOverviewResponse;
import dev.ktpm.model.ChatRoom;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IChatRoomService {
	ChatRoom getRoom(UUID roomUUID);
	
	List<ChatRoom> listPublicRooms(UUID requester, int limit);
	
	// rooms the user is in
	List<ChatRoom> listJoinedRooms(UUID requester, int limit);
	
	int getMemberCount(UUID roomUUID);
	
	// private + no inviteCode = we gen one, returns the created room
	ChatRoom createRoom(UUID owner, String name, String motd, boolean isPublic, Optional<String> inviteCode);
	
	void joinRoom(UUID userUUID, UUID roomUUID, Optional<String> inviteCode);
	
	// join with an invite code/link without knowing the room uuid
	ChatRoom joinByInviteCode(UUID userUUID, String inviteCode);
	
	void leaveRoom(UUID userUUID, UUID roomUUID,
		boolean silentLeave, Optional<String> reason
	);
	
	// returns the members at deletion time so they can be notified
	List<UUID> deleteRoom(
		UUID requesterUUID,
		UUID roomUUID
	);
	
	// requester null = not logged in (public rooms only)
	RoomOverviewResponse getOverview(UUID requester, UUID roomUUID);
	
	// preview a room from an invite link (no login, no member list)
	RoomOverviewResponse getOverviewByInviteCode(String inviteCode);
	
	// needs MANAGE_ROOM. Optional.empty fields stay as they are
	ChatRoom updateRoom(UUID actor, UUID roomUUID, Optional<String> name, Optional<String> motd);
	
	// needs MANAGE_ROOM. images only, saved to serveravatars/<room uuid>.<ext>
	ChatRoom setAvatar(UUID actor, UUID roomUUID, byte[] data, String originalName);
	
	ChatRoom removeAvatar(UUID actor, UUID roomUUID);
	
	// needs MANAGE_ROOM, private rooms only. no code = generate one
	ChatRoom resetInviteCode(UUID actor, UUID roomUUID, Optional<String> inviteCode);
}
