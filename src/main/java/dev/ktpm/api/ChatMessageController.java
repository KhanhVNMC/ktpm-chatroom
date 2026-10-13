package dev.ktpm.api;

import java.net.URI;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ktpm.model.AttachmentAsset;
import dev.ktpm.model.ChatMessage;
import dev.ktpm.service.IAuthenticationService;
import dev.ktpm.service.IChatMessageService;
import dev.ktpm.service.IUserService;
import io.javalin.http.Context;
import io.javalin.router.JavalinDefaultRoutingApi;

public class ChatMessageController {
	private final IChatMessageService messageService;
	private final IAuthenticationService authService;
	private final IUserService userService;
	
	public ChatMessageController(IChatMessageService messageService, IAuthenticationService authService, IUserService userService) {
		this.messageService = messageService;
		this.authService = authService;
		this.userService = userService;
	}
	
	public void registerRoutes(JavalinDefaultRoutingApi router) {
		router.get("/api/chatrooms/{id}/history", Sputnik.handle(this::getHistory));
		router.post("/api/chatrooms/{id}/messages", Sputnik.handle(this::sendMessage));
		router.patch("/api/chatrooms/{id}/messages/{messageId}", Sputnik.handle(this::editMessage));
		router.delete("/api/chatrooms/{id}/messages/{messageId}", Sputnik.handle(this::deleteMessage));
	}
	
	// login ko bắt buộc (phòng public ai cũng xem được)
	public void getHistory(Context ctx) {
		UUID requester = Sputnik.optionalAuth(ctx, authService);
		Optional<UUID> before = Optional.ofNullable(ctx.queryParam("before_message")).map(UUID::fromString);
		var messages = new ArrayList<>(messageService.getHistory(
			requester, UUID.fromString(ctx.pathParam("id")), 
			Sputnik.limit(ctx), before
		));
		Collections.reverse(messages); // repo trả newest -> oldest, ADML muốn old -> new
		var names = new HashMap<UUID, String>();
		ctx.json(Sputnik.obj("messages", messages.stream().map(m -> toJson(m, names)).toList()));
	}
	
	public void sendMessage(Context ctx) {
		UUID sender = Sputnik.requireAuth(ctx, authService);
		var body = Sputnik.body(ctx);
		messageService.sendMessage(
			sender, UUID.fromString(ctx.pathParam("id")),
			Sputnik.text(body, "message_content"), 
			uploadedFiles(body)
		);
		ctx.json(Sputnik.obj());
	}
	
	public void editMessage(Context ctx) {
		UUID requester = Sputnik.requireAuth(ctx, authService);
		var body = Sputnik.body(ctx);
		messageService.editMessage(
			requester, UUID.fromString(ctx.pathParam("id")), UUID.fromString(ctx.pathParam("messageId")),
			Sputnik.text(body, "new_content"), body.path("keep_attachments").asBoolean(false)
		);
		ctx.json(Sputnik.obj());
	}
	
	public void deleteMessage(Context ctx) {
		UUID requester = Sputnik.requireAuth(ctx, authService);
		messageService.deleteMessage(
			requester, UUID.fromString(ctx.pathParam("id")), UUID.fromString(ctx.pathParam("messageId"))
		);
		ctx.json(Sputnik.obj());
	}
	
	// uploaded_files: [{attachment: <base64>, size}]. File CHƯA được lưu đâu cả (stub), chỉ sinh uuid cho mỗi file
	private Optional<List<AttachmentAsset>> uploadedFiles(JsonNode body) {
		if (!body.hasNonNull("uploaded_files")) {
			return Optional.empty();
		}
		var assets = new ArrayList<AttachmentAsset>();
		for (JsonNode file : body.get("uploaded_files")) {
			byte[] bytes = Base64.getDecoder().decode(file.path("attachment").asText(""));
			assets.add(new AttachmentAsset(UUID.randomUUID(), bytes, file.path("size").asLong(bytes.length)));
		}
		return Optional.of(assets);
	}
	
	private Map<String, Object> toJson(ChatMessage message, Map<UUID, String> names) {
		return Sputnik.obj(
			"chatMessageUUID", message.chatMessageUUID(),
			"sender", names.computeIfAbsent(message.sender(), this::senderName),
			"content", message.content(),
			"attachments", message.attachments().stream().map(URI::toString).toList(),
			"unixTimestamp", message.sentTimestamp() // millis
		);
	}
	
	private String senderName(UUID userUUID) {
		try {
			var user = userService.getPublicUser(null, userUUID);
			return user.displayName() != null ? user.displayName() : user.username();
		} catch (IllegalArgumentException e) {
			return userUUID.toString(); // user delted
		}
	}
}
