package dev.ktpm.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import dev.ktpm.api.dto.RoomOverviewResponse;
import dev.ktpm.consts.RealtimeEventType;
import dev.ktpm.model.ChatRoom;
import dev.ktpm.service.IAssetService;
import dev.ktpm.service.IAuthenticationService;
import dev.ktpm.service.IChatRoomService;
import dev.ktpm.service.IUserService;
import io.javalin.http.Context;
import io.javalin.router.JavalinDefaultRoutingApi;

public class ChatRoomController {
	private final IChatRoomService chatRoomService;
	private final IAuthenticationService authService;
	private final IUserService userService;
	private final IAssetService assetService;
	private final SSEventDispatcher dispatcher;

	public ChatRoomController(
		IChatRoomService chatRoomService, IAuthenticationService authService,
		IUserService userService, IAssetService assetService, SSEventDispatcher dp
	) {
		this.chatRoomService = chatRoomService;
		this.authService = authService;
		this.userService = userService;
		this.assetService = assetService;
		this.dispatcher = dp;
	}

	public void registerRoutes(JavalinDefaultRoutingApi router) {
		router.post("/api/chatrooms", Sputnik.handle(this::createRoom));
		router.get("/api/chatrooms/listing", Sputnik.handle(this::listPublicRooms));
		router.get("/api/chatrooms/mine", Sputnik.handle(this::listMyRooms));
		router.get("/api/chatrooms/invite/{code}", Sputnik.handle(this::previewInvite));
		router.post("/api/chatrooms/invite/{code}/join", Sputnik.handle(this::joinByInvite));
		router.get("/api/chatrooms/{id}/overview", Sputnik.handle(this::getOverview));
		router.patch("/api/chatrooms/{id}", Sputnik.handle(this::updateRoom));
		router.delete("/api/chatrooms/{id}", Sputnik.handle(this::deleteRoom));
		router.get("/api/chatrooms/{id}/avatar", Sputnik.handle(this::getAvatar));
		router.put("/api/chatrooms/{id}/avatar", Sputnik.handle(this::setAvatar));
		router.delete("/api/chatrooms/{id}/avatar", Sputnik.handle(this::removeAvatar));
		router.post("/api/chatrooms/{id}/invite-code", Sputnik.handle(this::resetInviteCode));
		router.post("/api/chatrooms/{id}/join", Sputnik.handle(this::joinRoom));
		router.post("/api/chatrooms/{id}/leave", Sputnik.handle(this::leaveRoom));
	}

	// body {name, motd, is_public (default true), invite_code?} -> {room_uuid, invite_code, invite_link}
	public void createRoom(Context ctx) {
		UUID owner = Sputnik.authenticatedUser(ctx);
		var body = Sputnik.body(ctx);
		var room = chatRoomService.createRoom(
			owner, Sputnik.text(body, "name"), Sputnik.text(body, "motd"),
			body.path("is_public").asBoolean(true), Sputnik.optText(body, "invite_code")
		);
		ctx.status(201);
		ctx.json(Sputnik.obj(
			"room_uuid", room.uuid(), 
			"invite_code", room.inviteCode(), 
			"invite_link", inviteLink(room)
		));
	}
	
	// login ko bắt buộc
	public void listPublicRooms(Context ctx) {
		UUID requester = Sputnik.optionalAuthenticatedUser(ctx);
		var rooms = chatRoomService.listPublicRooms(requester, Sputnik.limit(ctx)).stream()
			.map(room -> Sputnik.room(room, chatRoomService.getMemberCount(room.uuid())))
		.toList();
		ctx.json(Sputnik.obj("public_rooms", rooms));
	}
	
	// rooms (private too) that im in
	public void listMyRooms(Context ctx) {
		UUID requester = Sputnik.authenticatedUser(ctx);
		var rooms = chatRoomService.listJoinedRooms(requester, Sputnik.limit(ctx)).stream()
			.map(room -> Sputnik.room(room, chatRoomService.getMemberCount(room.uuid())))
		.toList();
		ctx.json(Sputnik.obj("rooms", rooms));
	}
	
	// public rooms are open to everyone, private ones need membership
	public void getOverview(Context ctx) {
		var overview = chatRoomService.getOverview(
				Sputnik.optionalAuthenticatedUser(ctx), UUID.fromString(ctx.pathParam("id"))
		);
		ctx.json(overviewJson(overview));
	}
	
	// preview from an invite link, no login needed
	public void previewInvite(Context ctx) {
		ctx.json(overviewJson(chatRoomService.getOverviewByInviteCode(ctx.pathParam("code"))));
	}
	
	public void joinRoom(Context ctx) {
		UUID user = Sputnik.authenticatedUser(ctx);
		var body = Sputnik.body(ctx);
		UUID roomUUID = UUID.fromString(ctx.pathParam("id"));
		chatRoomService.joinRoom(user, roomUUID, Sputnik.optText(body, "room_invite_code"));
		announceJoin(roomUUID, user);
		ctx.json(Sputnik.obj());
	}
	
	public void joinByInvite(Context ctx) {
		UUID user = Sputnik.authenticatedUser(ctx);
		ChatRoom room = chatRoomService.joinByInviteCode(user, ctx.pathParam("code"));
		announceJoin(room.uuid(), user);
		ctx.json(Sputnik.obj("room_uuid", room.uuid()));
	}
	
	// silent_leave: leave quietly, nobody else is told
	public void leaveRoom(Context ctx) {
		UUID user = Sputnik.authenticatedUser(ctx);
		var body = Sputnik.body(ctx);
		UUID roomUUID = UUID.fromString(ctx.pathParam("id"));
		boolean silent = body.path("silent_leave").asBoolean(false);
		var reason = Sputnik.optText(body, "reason");
		chatRoomService.leaveRoom(user, roomUUID, silent, reason);

		var payload = Sputnik.obj(
			"room_uuid", roomUUID, 
			"user", Sputnik.user(userService.getPublicUser(user, user)),
			"reason", silent ? null : reason.orElse(null)
		);
		if (silent) {
			dispatcher.sendToUsers(List.of(user), RealtimeEventType.MEMBER_LEFT, payload);
		} else {
			dispatcher.sendToRoom(roomUUID, RealtimeEventType.MEMBER_LEFT, payload, user);
		}
		ctx.json(Sputnik.obj());
	}

	public void deleteRoom(Context ctx) {
		UUID user = Sputnik.authenticatedUser(ctx);
		UUID roomUUID = UUID.fromString(ctx.pathParam("id"));
		var members = chatRoomService.deleteRoom(user, roomUUID);
		dispatcher.sendToUsers(members, RealtimeEventType.ROOM_DELETED, 
			Sputnik.obj("room_uuid", roomUUID)
		);
		ctx.json(Sputnik.obj());
	}
	
	// body {name?, motd?}, missing field = keep as is
	public void updateRoom(Context ctx) {
		UUID user = Sputnik.authenticatedUser(ctx);
		var body = Sputnik.body(ctx);
		var room = chatRoomService.updateRoom(
			user, UUID.fromString(ctx.pathParam("id")), Sputnik.optText(body, "name"), Sputnik.optText(body, "motd")
		);
		announceRoomUpdate(room);
		ctx.json(Sputnik.room(room, chatRoomService.getMemberCount(room.uuid())));
	}
	
	// no auth, <img> cant send headers
	public void getAvatar(Context ctx) {
		ChatRoom room = chatRoomService.getRoom(UUID.fromString(ctx.pathParam("id")));
		if (room.avatar() == null) {
			throw new APIException(404, "Room has no avatar");
		}
		Sputnik.sendFile(ctx, room.avatar(), assetService.contentType(room.avatar()));
	}
	
	// raw body (+ X-File-Name header) or multipart field "file"
	public void setAvatar(Context ctx) {
		UUID user = Sputnik.authenticatedUser(ctx);
		var file = Sputnik.upload(ctx);
		var room = chatRoomService.setAvatar(user, UUID.fromString(ctx.pathParam("id")), file.data(), file.originalName());
		announceRoomUpdate(room);
		ctx.json(Sputnik.room(room, chatRoomService.getMemberCount(room.uuid())));
	}
	
	public void removeAvatar(Context ctx) {
		UUID user = Sputnik.authenticatedUser(ctx);
		var room = chatRoomService.removeAvatar(user, UUID.fromString(ctx.pathParam("id")));
		announceRoomUpdate(room);
		ctx.json(Sputnik.room(room, chatRoomService.getMemberCount(room.uuid())));
	}
	
	// body {invite_code?}: replaces the old code (old one stops working), no code = generate one
	public void resetInviteCode(Context ctx) {
		UUID user = Sputnik.authenticatedUser(ctx);
		var room = chatRoomService.resetInviteCode(
			user, UUID.fromString(ctx.pathParam("id")), Sputnik.optText(Sputnik.body(ctx), "invite_code")
		);
		ctx.json(Sputnik.obj(
			"invite_code", room.inviteCode(), 
			"invite_link", inviteLink(room)
		));
	}
	
	private void announceJoin(UUID roomUUID, UUID user) {
		dispatcher.sendToRoom(roomUUID, RealtimeEventType.MEMBER_JOINED, Sputnik.obj(
			"room_uuid", roomUUID, "user", Sputnik.user(userService.getPublicUser(user, user))
		));
	}
	
	private void announceRoomUpdate(ChatRoom room) {
		dispatcher.sendToRoom(room.uuid(), RealtimeEventType.ROOM_UPDATED, Sputnik.room(room, chatRoomService.getMemberCount(room.uuid())));
	}
	
	private static String inviteLink(ChatRoom room) {
		return room.inviteCode() == null ? null : "/api/chatrooms/invite/" + room.inviteCode();
	}
	
	private static Map<String, Object> overviewJson(RoomOverviewResponse overview) {
		return Sputnik.obj(
			"roomUUID", overview.roomUUID(),
			"name", overview.name(),
			"motd", overview.motd(),
			"avatar", overview.avatar() == null ? null : "/api/chatrooms/" + overview.roomUUID() + "/avatar",
			"createdAt", overview.createdAt(),
			"is_public", overview.isPublic(),
			"owner", overview.owner(),
			"owner_uuid", overview.ownerUUID(),
			"member_count", overview.memberCount(),
			"members", overview.members().stream().map(Sputnik::user).toList(),
			"invite_code", overview.inviteCode()
		);
	}
}
