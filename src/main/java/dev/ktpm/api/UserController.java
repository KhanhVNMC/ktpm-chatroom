package dev.ktpm.api;

import dev.ktpm.service.IAuthenticationService;
import dev.ktpm.service.IUserService;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.router.JavalinDefaultRoutingApi;

public class UserController {
	private final IUserService userService;
	private final IAuthenticationService authService;

	public UserController(IUserService userService, IAuthenticationService authService) {
		this.userService = userService;
		this.authService = authService;
	}

	public void registerRoutes(JavalinDefaultRoutingApi router) {
		router.get("/api/user/{id}", this::getUser);
	}

	public void getUser(Context ctx) {
		// TODO: Implement getUser
		ctx.status(HttpStatus.NOT_IMPLEMENTED);
	}
}
