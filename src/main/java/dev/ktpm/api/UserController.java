package dev.ktpm.api;

import java.util.UUID;

import dev.ktpm.service.IAuthenticationService;
import dev.ktpm.service.IUserService;
import io.javalin.http.Context;
import io.javalin.router.JavalinDefaultRoutingApi;

public class UserController {
	private final IUserService userService;
	private final IAuthenticationService authService;
	
	public UserController(IUserService userService, IAuthenticationService authService) {
		this.userService = userService;
		this.authService = authService;
	}
	
	public void registerRoutes(JavalinDefaultRoutingApi router) {
		router.get("/api/user/{id}", Sputnik.handle(this::getUser));
	}
	
	public void getUser(Context ctx) {
		UUID requester = Sputnik.optionalAuth(ctx, authService);
		ctx.json(Sputnik.user(userService.getPublicUser(requester, UUID.fromString(ctx.pathParam("id")))));
	}
}
