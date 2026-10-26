package dev.ktpm.api;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.ktpm.api.dto.UploadedFile;
import dev.ktpm.model.ChatMessage;
import dev.ktpm.model.ChatRoom;
import dev.ktpm.model.User;
import dev.ktpm.service.IAssetService;
import dev.ktpm.service.IAuthenticationService;
import dev.ktpm.service.IUserService;
import io.javalin.http.Context;
import io.javalin.http.Handler;

public final class Sputnik {
	private static final ObjectMapper JSON = new ObjectMapper();
	
	public static Handler handle(Handler handler) {
		return ctx -> {
			try {
				handler.handle(ctx);
			} catch (APIException e) {
				fail(ctx, e.status, e.getMessage());
			} catch (IllegalArgumentException e) {
				String msg = e.getMessage();
				fail(ctx, msg != null && msg.toLowerCase().contains("not found") ? 404 : 400, msg);
			} catch (IllegalStateException e) {
				fail(ctx, 403, e.getMessage());
			}
		};
	}
	
	private static void fail(Context ctx, int status, String message) {
		ctx.status(status);
		ctx.json(obj("error_message", message));
	}
	
	public static String token(Context ctx) {
		String header = ctx.header("Authorization");
		return header != null && header.startsWith("Bearer ") ? header.substring(7).trim() : null;
	}

	// xac thuc middleware
	public static UUID authenticatedUser(Context ctx) {
		UUID user = ctx.attribute(AuthenticationMiddleware.AUTHENTICATED_USER);

		if (user == null) {
			throw new APIException(401, "Authentication required");
		}

		return user;
	}

	public static UUID optionalAuthenticatedUser(Context ctx) {
		return ctx.attribute(AuthenticationMiddleware.AUTHENTICATED_USER);
	}

	/** null nếu ko gửi token, 401 nếu token sai/hết hạn */
	public static UUID optionalAuth(Context ctx, IAuthenticationService authService) {
		String token = token(ctx);
		if (token == null) {
			return null;
		}
		UUID user = authService.authenticate(token);
		if (user == null) {
			throw new APIException(401, "Invalid or expired session");
		}
		return user;
	}
	
	public static UUID requireAuth(Context ctx, IAuthenticationService authService) {
		UUID user = optionalAuth(ctx, authService);
		if (user == null) {
			throw new APIException(401, "Authentication required");
		}
		return user;
	}
	
	public static JsonNode body(Context ctx) {
		String raw = ctx.body();
		if (raw == null || raw.isBlank()) {
			return JSON.createObjectNode();
		}
		try {
			return JSON.readTree(raw);
		} catch (JsonProcessingException e) {
			throw new APIException(400, "Malformed JSON body");
		}
	}
	
	public static String text(JsonNode body, String field) {
		return body.path(field).isTextual() ? body.get(field).asText() : null;
	}
	
	public static String require(JsonNode body, String field) {
		String value = text(body, field);
		if (value == null) {
			throw new APIException(400, field + " is required");
		}
		return value;
	}
	
	public static Optional<String> optText(JsonNode body, String field) {
		return Optional.ofNullable(text(body, field));
	}
	
	public static int limit(Context ctx) {
		String raw = ctx.queryParam("limit");
		return raw == null ? 0 : Integer.parseInt(raw);
	}
	
	public static Map<String, Object> obj(Object... keyValues) {
		var map = new LinkedHashMap<String, Object>();
		for (int i = 0; i < keyValues.length; i += 2) {
			map.put((String) keyValues[i], keyValues[i + 1]);
		}
		return map;
	}
	
	public static Optional<UUID> optUUID(JsonNode body, String field) {
		return Optional.ofNullable(text(body, field)).map(UUID::fromString);
	}
	
	public static String assetUrl(UUID asset) {
		return asset == null ? null : IAssetService.uri(asset).toString();
	}
	
	public static String roomAvatarUrl(ChatRoom room) {
		return room.avatar() == null ? null : "/api/chatrooms/" + room.uuid() + "/avatar";
	}
	
	/** raw body (octet-stream) or multipart field "file", file name comes from the X-File-Name header */
	public static UploadedFile upload(Context ctx) {
		var multipart = ctx.uploadedFile("file");
		if (multipart != null) {
			try (var in = multipart.content()) {
				return new UploadedFile(in.readAllBytes(), multipart.filename());
			} catch (java.io.IOException e) {
				throw new APIException(400, "Cannot read uploaded file");
			}
		}
		return new UploadedFile(ctx.bodyAsBytes(), ctx.header("X-File-Name"));
	}
	
	/** send a file back. unknown types (not image/video/audio) are forced to download so uploaded html/js cant run */
	public static void sendFile(Context ctx, java.nio.file.Path file, String contentType) {
		if (!java.nio.file.Files.isRegularFile(file)) {
			throw new APIException(404, "File not found");
		}
		try {
			ctx.header("X-Content-Type-Options", "nosniff");
			ctx.header("Cache-Control", "no-cache");
			if (contentType.equals("application/octet-stream")) {
				ctx.header("Content-Disposition", "attachment");
			}
			ctx.contentType(contentType);
			ctx.result(java.nio.file.Files.newInputStream(file));
		} catch (java.io.IOException e) {
			throw new APIException(404, "File not found");
		}
	}
	
	public static Map<String, Object> user(User user) {
		return obj(
			"uuid", user.uuid(), "username", user.username(), "display_name", user.displayName(),
			"avatar", assetUrl(user.avatar()), "presence", user.presence(), "status_text", user.statusText()
		);
	}
	
	public static Map<String, Object> room(ChatRoom room, int members) {
		return obj(
			"roomUUID", room.uuid(), "name", room.name(), "motd", room.motd(),
			"avatar", roomAvatarUrl(room), "createdAt", room.createdAt(), "members", members
		);
	}
	
	/** names: uuid -> display name cache for one request (history has lots of messages from the same sender) */
	public static Map<String, Object> message(ChatMessage message, Map<UUID, String> names, IUserService users) {
		return obj(
			"chatMessageUUID", message.chatMessageUUID(),
			"roomUUID", message.chatRoomUUID(),
			"senderUUID", message.sender(),
			"sender", names.computeIfAbsent(message.sender(), uuid -> senderName(users, uuid)),
			"content", message.content(),
			"attachments", message.attachments().stream().map(URI::toString).toList(),
			"unixTimestamp", message.sentTimestamp() // millis
		);
	}
	
	private static String senderName(IUserService users, UUID userUUID) {
		try {
			var user = users.getPublicUser(null, userUUID);
			return user.displayName() != null ? user.displayName() : user.username();
		} catch (IllegalArgumentException e) {
			return userUUID.toString(); // user delted
		}
	}
}
