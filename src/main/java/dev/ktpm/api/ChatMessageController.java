package dev.ktpm.api;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ktpm.api.dto.UploadedFile;
import dev.ktpm.consts.RealtimeEventType;
import dev.ktpm.service.IAuthenticationService;
import dev.ktpm.service.IChatMessageService;
import dev.ktpm.service.IUserService;
import io.javalin.http.Context;
import io.javalin.router.JavalinDefaultRoutingApi;

public class ChatMessageController {
	private final IChatMessageService messageService;
	private final IAuthenticationService authService;
	private final IUserService userService;
	private final SSEventDispatcher dispatcher;

	public ChatMessageController(
		IChatMessageService messageService, IAuthenticationService authService,
		IUserService userService, SSEventDispatcher dp
	) {
		this.messageService = messageService;
		this.authService = authService;
		this.userService = userService;
		this.dispatcher = dp;
	}

	public void registerRoutes(JavalinDefaultRoutingApi router) {
		router.get("/api/chatrooms/{id}/history", Sputnik.handle(this::getHistory));
		router.get("/api/chatrooms/{id}/search", Sputnik.handle(this::searchMessages));
		router.post("/api/chatrooms/{id}/messages", Sputnik.handle(this::sendMessage));
		router.patch("/api/chatrooms/{id}/messages/{messageId}", Sputnik.handle(this::editMessage));
		router.delete("/api/chatrooms/{id}/messages/{messageId}", Sputnik.handle(this::deleteMessage));
	}

	// login ko bắt buộc (phòng public ai cũng xem được)
	public void getHistory(Context ctx) {
		UUID requester = Sputnik.optionalAuth(ctx, authService);
		var messages = new ArrayList<>(messageService.getHistory(
			requester, UUID.fromString(ctx.pathParam("id")),
			Sputnik.limit(ctx), beforeMessage(ctx)
		));
		ctx.json(Sputnik.obj("messages", toJson(messages)));
	}

	// GET q={text}&limit={LIMIT}&before_message={message_uuid}, like history
	public void searchMessages(Context ctx) {
		UUID requester = Sputnik.optionalAuth(ctx, authService);
		String query = ctx.queryParam("q");
		if (query == null || query.isBlank()) {
			throw new APIException(400, "q is required");
		}
		var messages = new ArrayList<>(messageService.search(
			requester, UUID.fromString(ctx.pathParam("id")), query,
			Sputnik.limit(ctx), beforeMessage(ctx)
		));
		ctx.json(Sputnik.obj("messages", toJson(messages)));
	}

	// uploaded_files: [{attachment: <base64>, file_name}], files get written to usercontent/ on the server
	public void sendMessage(Context ctx) {
		UUID sender = Sputnik.requireAuth(ctx, authService);
		var body = Sputnik.body(ctx);
		UUID roomUUID = UUID.fromString(ctx.pathParam("id"));
		var message = messageService.sendMessage(
			sender, roomUUID, Sputnik.text(body, "message_content"), uploadedFiles(body)
		);
		var json = Sputnik.message(message, new HashMap<>(), userService);
		dispatcher.sendToRoom(roomUUID, RealtimeEventType.MESSAGE_CREATED, json);
		ctx.status(201);
		ctx.json(json);
	}

	public void editMessage(Context ctx) {
		UUID requester = Sputnik.requireAuth(ctx, authService);
		var body = Sputnik.body(ctx);
		UUID roomUUID = UUID.fromString(ctx.pathParam("id"));
		var message = messageService.editMessage(
			requester, roomUUID, 
			UUID.fromString(ctx.pathParam("messageId")),
			Sputnik.text(body, "new_content"), 
			body.path("keep_attachments").asBoolean(false)
		);
		var json = Sputnik.message(message, new HashMap<>(), userService);
		dispatcher.sendToRoom(roomUUID, RealtimeEventType.MESSAGE_EDITED, json);
		ctx.json(json);
	}

	public void deleteMessage(Context ctx) {
		UUID requester = Sputnik.requireAuth(ctx, authService);
		UUID roomUUID = UUID.fromString(ctx.pathParam("id"));
		UUID messageUUID = UUID.fromString(ctx.pathParam("messageId"));
		messageService.deleteMessage(requester, roomUUID, messageUUID);
		dispatcher.sendToRoom(roomUUID, RealtimeEventType.MESSAGE_DELETED, Sputnik.obj(
			"room_uuid", roomUUID, 
			"message_uuid", messageUUID, 
			"deleted_by", requester
		));
		ctx.json(Sputnik.obj());
	}

	private Optional<UUID> beforeMessage(Context ctx) {
		return Optional.ofNullable(ctx.queryParam("before_message")).map(UUID::fromString);
	}

	// repo trả newest -> oldest, ADML muốn old -> new
	private List<java.util.Map<String, Object>> toJson(List<dev.ktpm.model.ChatMessage> messages) {
		Collections.reverse(messages);
		var names = new HashMap<UUID, String>();
		return messages.stream().map(m -> Sputnik.message(m, names, userService)).toList();
	}

	private Optional<List<UploadedFile>> uploadedFiles(JsonNode body) {
		if (!body.hasNonNull("uploaded_files")) {
			return Optional.empty();
		}
		var files = new ArrayList<UploadedFile>();
		for (JsonNode file : body.get("uploaded_files")) {
			files.add(new UploadedFile(
				Base64.getDecoder().decode(file.path("attachment").asText("")),
				Sputnik.text(file, "file_name")
			));
		}
		return Optional.of(files);
	}
}
