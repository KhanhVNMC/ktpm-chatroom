package dev.ktpm.api;

import java.util.UUID;

import dev.ktpm.consts.MemberAction;
import dev.ktpm.consts.RealtimeEventType;
import dev.ktpm.service.IRoomMemberService;
import dev.ktpm.service.IUserService;
import io.javalin.http.Context;
import io.javalin.router.JavalinDefaultRoutingApi;

public class RoomMemberController {
	private final IRoomMemberService memberService;
	private final IUserService userService;
	private final SSEventDispatcher hub;
	
	public RoomMemberController(
		IRoomMemberService memberService,
		IUserService userService, SSEventDispatcher hub
	) {
		this.memberService = memberService;
		this.userService = userService;
		this.hub = hub;
	}
	
	public void registerRoutes(JavalinDefaultRoutingApi router) {
		router.get("/api/chatrooms/{id}/members", Sputnik.handle(this::listMembers));
		router.post("/api/chatrooms/{id}/members/{userId}/{action}", Sputnik.handle(this::moderate));
		router.put("/api/chatrooms/{id}/members/{userId}/permissions", Sputnik.handle(this::setPermissions));
		router.post("/api/chatrooms/{id}/transfer-ownership", Sputnik.handle(this::transferOwnership));
	}
	
	// is_in_room = false == currently banned (only visible with BAN_MEMBER)
	public void listMembers(Context ctx) {
		UUID requester = Sputnik.authenticatedUser(ctx);
		var members = memberService.listMembers(requester, UUID.fromString(ctx.pathParam("id"))).stream()
		.map(m -> {
			var json = Sputnik.user(userService.getUser(m.userUUID()));
			json.put("joined_at", m.joinedAt());
			json.put("permission_bits", m.permissionBits());
			json.put("status_bits", m.statusBits());
			json.put("is_in_room", m.isInRoom());
			return json;
		}).toList();
		ctx.json(Sputnik.obj("members", members));
	}
	
	// POST .../members/{userId}/{kick|ban|unban|mute|unmute}, body {reason?}
	public void moderate(Context ctx) {
		UUID actor = Sputnik.authenticatedUser(ctx);
		UUID roomUUID = UUID.fromString(ctx.pathParam("id"));
		UUID target = UUID.fromString(ctx.pathParam("userId"));
		MemberAction action;
		try {
			action = MemberAction.valueOf(ctx.pathParam("action").toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new APIException(400, "Unknown action: " + ctx.pathParam("action"));
		}
		memberService.moderate(actor, roomUUID, target, action);

		// kick/ban target isnt a member anymore so send it to them separately
		hub.sendToRoom(roomUUID, action.event, Sputnik.obj(
			"room_uuid", roomUUID, 
			"user_uuid", target, 
			"actor_uuid", actor,
			"reason", Sputnik.text(Sputnik.body(ctx), "reason")
		), target);
		ctx.json(Sputnik.obj());
	}
	
	// body {permission_bits}, see RoomPermissions
	public void setPermissions(Context ctx) {
		UUID actor = Sputnik.authenticatedUser(ctx);
		UUID roomUUID = UUID.fromString(ctx.pathParam("id"));
		UUID target = UUID.fromString(ctx.pathParam("userId"));
		var bits = Sputnik.body(ctx).path("permission_bits");
		if (!bits.isInt()) {
			throw new APIException(400, "permission_bits is required");
		}
		memberService.setPermissions(actor, roomUUID, target, bits.asInt());
		hub.sendToRoom(roomUUID, RealtimeEventType.MEMBER_PERMISSIONS_CHANGED, Sputnik.obj(
			"room_uuid", roomUUID, 
			"user_uuid", target, 
			"actor_uuid", actor, 
			"permission_bits", bits.asInt()
		));
		ctx.json(Sputnik.obj());
	}
	
	// body {new_owner: <user uuid>}
	public void transferOwnership(Context ctx) {
		UUID actor = Sputnik.authenticatedUser(ctx);
		UUID roomUUID = UUID.fromString(ctx.pathParam("id"));
		UUID newOwner = UUID.fromString(Sputnik.require(Sputnik.body(ctx), "new_owner"));
		memberService.transferOwnership(actor, roomUUID, newOwner);
		hub.sendToRoom(roomUUID, RealtimeEventType.OWNER_TRANSFERRED, Sputnik.obj(
			"room_uuid", roomUUID, 
			"old_owner", actor, 
			"new_owner", newOwner
		));
		ctx.json(Sputnik.obj());
	}
}
