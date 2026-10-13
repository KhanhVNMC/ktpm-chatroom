package dev.ktpm.api;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.ktpm.model.User;
import dev.ktpm.service.IAuthenticationService;
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
	
	public static Map<String, Object> user(User user) {
		return obj("uuid", user.uuid(), "username", user.username(), "display_name", user.displayName());
	}
}
