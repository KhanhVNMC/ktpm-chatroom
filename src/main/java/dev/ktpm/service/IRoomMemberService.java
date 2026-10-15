package dev.ktpm.service;

import dev.ktpm.consts.MemberAction;
import dev.ktpm.model.RoomMembership;

import java.util.List;
import java.util.UUID;

public interface IRoomMemberService {
	// requester must be in the room. banned people (isInRoom = false) only show up for BAN_MEMBERS
	List<RoomMembership> listMembers(UUID requester, UUID roomUUID);

	// kick/ban/unban/mute/unmute. cant touch the owner, and only the owner can touch admins
	void moderate(UUID actor, UUID roomUUID, UUID target, MemberAction action);

	// overwrite a member's permission bits (needs GRANT_PERMISSIONS)
	void setPermissions(UUID actor, UUID roomUUID, UUID target, int permissionBits);

	// owner only. the old owner becomes a normal member
	void transferOwnership(UUID actor, UUID roomUUID, UUID newOwner);
}
