package dev.ktpm.api;

import dev.ktpm.service.IAuthenticationService;
import dev.ktpm.service.IUserService;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.router.JavalinDefaultRoutingApi;

public class AuthController {
	private final IAuthenticationService authService;
	private final IUserService userService;

	public AuthController(IAuthenticationService authService, IUserService userService) {
		this.authService = authService;
		this.userService = userService;
	}

	public void registerRoutes(JavalinDefaultRoutingApi router) {
		router.post("/api/auth/login", this::login);
		router.post("/api/auth/logout", this::logout);
		router.get("/api/auth/me", this::getMe);
	}

	public void login(Context ctx) {
		// TODO: Implement login
		ctx.status(HttpStatus.NOT_IMPLEMENTED);
	}

	public void logout(Context ctx) {
		// TODO: Implement logout
		ctx.status(HttpStatus.NOT_IMPLEMENTED);
	}

	public void getMe(Context ctx) {
		// TODO: Implement getMe
		ctx.status(HttpStatus.NOT_IMPLEMENTED);
	}
}
