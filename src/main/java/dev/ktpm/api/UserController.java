package dev.ktpm.api;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ktpm.api.dto.UpdateProfileRequest;
import dev.ktpm.consts.RealtimeEventType;
import dev.ktpm.consts.UserPresence;
import dev.ktpm.model.User;
import dev.ktpm.service.IAssetService;
import dev.ktpm.service.IUserService;
import io.javalin.http.Context;
import io.javalin.router.JavalinDefaultRoutingApi;

public class UserController {
	private final IUserService userService;
	private final IAssetService assetService;
	private final SSEventDispatcher hub;
	
	public UserController(IUserService userService, IAssetService assetService, SSEventDispatcher hub) {
		this.userService = userService;
		this.assetService = assetService;
		this.hub = hub;
	}
	
	public void registerRoutes(JavalinDefaultRoutingApi router) {
		router.patch("/api/user/me", Sputnik.handle(this::updateMe));
		router.put("/api/user/me/avatar", Sputnik.handle(this::setAvatar));
		router.delete("/api/user/me/avatar", Sputnik.handle(this::removeAvatar));
		router.get("/api/user/{id}", Sputnik.handle(this::getUser));
		router.get("/api/user/{id}/avatar", Sputnik.handle(this::getAvatar));
	}
	
	public void getUser(Context ctx) {
		ctx.json(Sputnik.user(userService.getUser(UUID.fromString(ctx.pathParam("id")))));
	}
	
	// body (missing field = keep as is): {display_name, presence, status_text}
	public void updateMe(Context ctx) {
		UUID requester = Sputnik.authenticatedUser(ctx);
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
			presence, 
			Sputnik.optText(body, "status_text")
		));
		this.announceUserUpdate(ctx, user);
	}
	
	// no auth, <img> cant send headers
	public void getAvatar(Context ctx) {
		User user = userService.getUser(UUID.fromString(ctx.pathParam("id")));
		if (user.avatar() == null) {
			throw new APIException(404, "User has no avatar");
		}
		Sputnik.sendFile(ctx, user.avatar(), assetService.contentType(user.avatar()));
	}
	
	// raw body (+ X-File-Name header) or multipart field "file"
	public void setAvatar(Context ctx) {
		UUID requester = Sputnik.authenticatedUser(ctx);
		var file = Sputnik.upload(ctx);
		this.announceUserUpdate(ctx, userService.setAvatar(requester, file.data(), file.originalName()));
	}
	
	public void removeAvatar(Context ctx) {
		UUID requester = Sputnik.authenticatedUser(ctx);
		this.announceUserUpdate(ctx, userService.removeAvatar(requester));
	}
	
	private void announceUserUpdate(Context ctx, User user) {
		var json = Sputnik.user(user);
		hub.sendToPeers(user.uuid(), RealtimeEventType.USER_UPDATED, json);
		ctx.json(json);
	}
}
