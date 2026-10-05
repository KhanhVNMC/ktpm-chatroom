package dev.ktpm.api;

import dev.ktpm.service.IAuthenticationService;
import dev.ktpm.service.IChatRoomService;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.router.JavalinDefaultRoutingApi;

public class ChatRoomController {

	private final IChatRoomService chatRoomService;
	private final IAuthenticationService authService;

	public ChatRoomController(IChatRoomService chatRoomService, IAuthenticationService authService) {
		this.chatRoomService = chatRoomService;
		this.authService = authService;
	}

	public void registerRoutes(JavalinDefaultRoutingApi router) {
		router.get("/api/rooms", this::listPublicRooms);
		router.get("/api/rooms/{id}", this::getOverview);
		router.post("/api/rooms/{id}/join", this::joinRoom);
		router.post("/api/rooms/{id}/leave", this::leaveRoom);
	}

	public void listPublicRooms(Context ctx) {
		// TODO: Implement listPublicRooms
		ctx.status(HttpStatus.NOT_IMPLEMENTED);
	}

	public void getOverview(Context ctx) {
		// TODO: Implement getOverview
		ctx.status(HttpStatus.NOT_IMPLEMENTED);
	}

	public void joinRoom(Context ctx) {
		// TODO: Implement joinRoom
		ctx.status(HttpStatus.NOT_IMPLEMENTED);
	}

	public void leaveRoom(Context ctx) {
		// TODO: Implement leaveRoom
		ctx.status(HttpStatus.NOT_IMPLEMENTED);
	}
}
