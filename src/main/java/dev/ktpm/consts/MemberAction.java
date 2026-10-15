package dev.ktpm.consts;

// ky luat shit on a member, with the event they fire over sse
public enum MemberAction {
	KICK(RealtimeEventType.MEMBER_KICKED),
	BAN(RealtimeEventType.MEMBER_BANNED),
	UNBAN(RealtimeEventType.MEMBER_UNBANNED),
	MUTE(RealtimeEventType.MEMBER_MUTED),
	UNMUTE(RealtimeEventType.MEMBER_UNMUTED);
	
	public final RealtimeEventType event;
	
	MemberAction(RealtimeEventType event) {
		this.event = event;
	}
}
