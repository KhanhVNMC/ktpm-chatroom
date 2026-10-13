package dev.ktpm.service;

import dev.ktpm.api.dto.RoomOverviewResponse;
import dev.ktpm.model.ChatRoom;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IChatRoomService {
	List<ChatRoom> listPublicRooms(UUID requester, int limit);
	
	int getMemberCount(UUID roomUUID);
	
	// private room ko truyền inviteCode thì tự sinh. Trả về room đã tạo
	ChatRoom createRoom(UUID owner, String motd, boolean isPublic, Optional<String> inviteCode);
	
	void joinRoom(UUID userUUID, UUID roomUUID, Optional<String> inviteCode);
	
	void leaveRoom(UUID userUUID, UUID roomUUID,
		boolean silentLeave, Optional<String> reason
	);

	void deleteRoom(
			UUID requesterUUID,
			UUID roomUUID
	);

	RoomOverviewResponse getOverview(UUID requester, UUID roomUUID);
}
