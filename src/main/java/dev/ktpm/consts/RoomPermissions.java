package dev.ktpm.consts;

public final class RoomPermissions {
	public static final int SEND_MESSAGES = 1 << 0;
	public static final int EDIT_MESSAGES = 1 << 1;
	public static final int DELETE_MESSAGES = 1 << 2;
	public static final int KICK_MEMBERS = 1 << 3;
	public static final int BAN_MEMBERS = 1 << 4;
	public static final int MANAGE_ROOM = 1 << 5;
	public static final int MUTE_MEMBERS = 1 << 6; // mute + unmute
	// "admin" permission: edit other people's permissions, only the owner can hand ts out
	public static final int GRANT_PERMISSIONS = 1 << 7;
	
	public static final int ALL = SEND_MESSAGES | EDIT_MESSAGES | DELETE_MESSAGES | KICK_MEMBERS | BAN_MEMBERS | MANAGE_ROOM | MUTE_MEMBERS | GRANT_PERMISSIONS;
	public static final int DEFAULT_MEMBER = SEND_MESSAGES | EDIT_MESSAGES;
}
