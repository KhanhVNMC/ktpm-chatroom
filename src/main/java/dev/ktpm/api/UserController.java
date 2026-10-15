package dev.ktpm.api;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ktpm.api.dto.UpdateProfileRequest;
import dev.ktpm.consts.RealtimeEventType;
import dev.ktpm.consts.UserPresence;
import dev.ktpm.service.IAuthenticationService;
import dev.ktpm.service.IUserService;
import io.javalin.http.Context;
import io.javalin.router.JavalinDefaultRoutingApi;

public class UserController {
	private final IUserService userService;
	private final IAuthenticationService authService;
	private final SSEventDispatcher hub;
	
	public UserController(IUserService userService, IAuthenticationService authService, SSEventDispatcher hub) {
		this.userService = userService;
		this.authService = authService;
		this.hub = hub;
	}
	
	public void registerRoutes(JavalinDefaultRoutingApi router) {
		router.patch("/api/user/me", Sputnik.handle(this::updateMe));
		router.get("/api/user/{id}", Sputnik.handle(this::getUser));
	}
	
	public void getUser(Context ctx) {
		UUID requester = Sputnik.optionalAuth(ctx, authService);
		ctx.json(Sputnik.user(userService.getPublicUser(requester, UUID.fromString(ctx.pathParam("id")))));
	}
	
	// body (missing field = keep as is): {display_name, avatar: <asset uuid> | null to remove, presence, status_text}
	// avatar is an asset uploaded through POST /api/assets
	public void updateMe(Context ctx) {
		UUID requester = Sputnik.requireAuth(ctx, authService);
		JsonNode body = Sputnik.body(ctx);
		
		var presence = Sputnik.optText(body, "presence").map(value -> {
			try {
				return UserPresence.valueOf(value.toUpperCase());
			} catch (IllegalArgumentException e) {
				throw new APIException(400, "presence is invalid");
			}
		});
		var user = userService.updateProfile(requester, new UpdateProfileRequest(
			Sputnik.optText(body, "display_name"),
			Sputnik.optUUID(body, "avatar"), body.has("avatar") && body.get("avatar").isNull(),
			presence, Sputnik.optText(body, "status_text")
		));
		var json = Sputnik.user(user);
		hub.sendToPeers(requester, RealtimeEventType.USER_UPDATED, json);
		ctx.json(json);
	}
}
