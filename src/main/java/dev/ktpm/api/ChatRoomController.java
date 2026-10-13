package dev.ktpm.api;

import java.util.UUID;

import dev.ktpm.service.IAuthenticationService;
import dev.ktpm.service.IChatRoomService;
import io.javalin.http.Context;
import io.javalin.router.JavalinDefaultRoutingApi;

public class ChatRoomController {
	private final IChatRoomService chatRoomService;
	private final IAuthenticationService authService;
	
	public ChatRoomController(IChatRoomService chatRoomService, IAuthenticationService authService) {
		this.chatRoomService = chatRoomService;
		this.authService = authService;
	}
	
	public void registerRoutes(JavalinDefaultRoutingApi router) {
		router.post("/api/chatrooms", Sputnik.handle(this::createRoom));
		router.get("/api/chatrooms/listing", Sputnik.handle(this::listPublicRooms));
		router.get("/api/chatrooms/{id}/overview", Sputnik.handle(this::getOverview));
		router.post("/api/chatrooms/{id}/join", Sputnik.handle(this::joinRoom));
		router.post("/api/chatrooms/{id}/leave", Sputnik.handle(this::leaveRoom));
	}
	
	// ngoài ADML: body {motd, is_public (default true), invite_code?} -> {room_uuid, invite_code}
	public void createRoom(Context ctx) {
		UUID owner = Sputnik.requireAuth(ctx, authService);
		var body = Sputnik.body(ctx);
		var room = chatRoomService.createRoom(
			owner, Sputnik.text(body, "motd"), body.path("is_public").asBoolean(true), Sputnik.optText(body, "invite_code")
		);
		ctx.status(201);
		ctx.json(Sputnik.obj("room_uuid", room.uuid(), "invite_code", room.inviteCode()));
	}

	// login ko bắt buộc
	public void listPublicRooms(Context ctx) {
		UUID requester = Sputnik.optionalAuth(ctx, authService);
		var rooms = chatRoomService.listPublicRooms(requester, Sputnik.limit(ctx)).stream()
		.map(room -> Sputnik.obj(
			"roomUUID", room.uuid(),
			"motd", room.motd(),
			"members", chatRoomService.getMemberCount(room.uuid())
		)).toList();
		ctx.json(Sputnik.obj("public_rooms", rooms));
	}
	
	public void getOverview(Context ctx) {
		var overview = chatRoomService.getOverview(
			Sputnik.requireAuth(ctx, authService), UUID.fromString(ctx.pathParam("id"))
		);
		ctx.json(Sputnik.obj(
			"motd", overview.motd(),
			"owner", overview.owner(),
			"member_names", overview.memberNames()
		));
	}
	
	public void joinRoom(Context ctx) {
		UUID user = Sputnik.requireAuth(ctx, authService);
		var body = Sputnik.body(ctx);
		chatRoomService.joinRoom(user, UUID.fromString(ctx.pathParam("id")), Sputnik.optText(body, "room_invite_code"));
		ctx.json(Sputnik.obj());
	}
	
	public void leaveRoom(Context ctx) {
		UUID user = Sputnik.requireAuth(ctx, authService);
		var body = Sputnik.body(ctx);
		chatRoomService.leaveRoom(
			user, UUID.fromString(ctx.pathParam("id")),
			body.path("silent_leave").asBoolean(false), Sputnik.optText(body, "reason")
		);
		ctx.json(Sputnik.obj());
	}
}
