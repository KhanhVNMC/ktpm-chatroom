package dev.ktpm.api;

import dev.ktpm.service.IAuthenticationService;
import dev.ktpm.service.IChatMessageService;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.router.JavalinDefaultRoutingApi;

public class ChatMessageController {
	private final IChatMessageService messageService;
	private final IAuthenticationService authService;

	public ChatMessageController(IChatMessageService messageService, IAuthenticationService authService) {
		this.messageService = messageService;
		this.authService = authService;
	}

	public void registerRoutes(JavalinDefaultRoutingApi router) {
		router.get("/api/rooms/{id}/messages", this::getHistory);
		router.post("/api/rooms/{id}/messages", this::sendMessage);
		router.patch("/api/rooms/{id}/messages/{messageId}", this::editMessage);
		router.delete("/api/rooms/{id}/messages/{messageId}", this::deleteMessage);
	}

	public void getHistory(Context ctx) {
		// TODO: Implement getHistory
		ctx.status(HttpStatus.NOT_IMPLEMENTED);
	}

	public void sendMessage(Context ctx) {
		// TODO: Implement sendMessage
		ctx.status(HttpStatus.NOT_IMPLEMENTED);
	}

	public void editMessage(Context ctx) {
		// TODO: Implement editMessage
		ctx.status(HttpStatus.NOT_IMPLEMENTED);
	}

	public void deleteMessage(Context ctx) {
		// TODO: Implement deleteMessage
		ctx.status(HttpStatus.NOT_IMPLEMENTED);
	}
}
