package dev.ktpm.api;

import java.util.UUID;

import dev.ktpm.consts.RealtimeEventType;
import dev.ktpm.service.IAuthenticationService;
import dev.ktpm.service.IUserService;
import io.javalin.http.Context;
import io.javalin.router.JavalinDefaultRoutingApi;

public class AuthController {
	private final IAuthenticationService authService;
	private final IUserService userService;
	private final SSEventDispatcher dispatcher;
	
	public AuthController(IAuthenticationService authService, IUserService userService, SSEventDispatcher dp) {
		this.authService = authService;
		this.userService = userService;
		this.dispatcher = dp;
	}
	
	public void registerRoutes(JavalinDefaultRoutingApi router) {
		router.post("/api/auth/register", Sputnik.handle(this::register));
		router.post("/api/auth/login", Sputnik.handle(this::login));
		router.post("/api/auth/logout", Sputnik.handle(this::logout));
		router.get("/api/auth/me", Sputnik.handle(this::getMe));
	}
	
	public void register(Context ctx) {
		var body = Sputnik.body(ctx);
		String username = Sputnik.require(body, "username");
		String displayName = Sputnik.text(body, "display_name");
		var user = authService.register(username,
			displayName != null ? displayName : username,
			Sputnik.require(body, "password")
		);
		ctx.status(201);
		ctx.json(Sputnik.user(user));
	}
	
	// body {username, password} -> {session_id}, các request sau gửi "Authorization: Bearer <session_id>"
	public void login(Context ctx) {
		var body = Sputnik.body(ctx);
		String sessionID = authService.login(Sputnik.require(body, "username"), Sputnik.require(body, "password"));
		if (sessionID == null) {
			throw new APIException(401, "Invalid username or password");
		}
		announcePresence(authService.authenticate(sessionID));
		ctx.json(Sputnik.obj("session_id", sessionID));
	}
	
	public void logout(Context ctx) {
		String token = Sputnik.token(ctx);
		if (token == null) {
			throw new APIException(401, "Authentication required");
		}
		UUID user = Sputnik.authenticatedUser(ctx);
		authService.logout(token);
		dispatcher.disconnectSession(token); // also close the SSE streams of this session
		announcePresence(user);
		ctx.json(Sputnik.obj());
	}
	
	public void getMe(Context ctx) {
		ctx.json(Sputnik.user(userService.getPrivateUser(Sputnik.authenticatedUser(ctx))));
	}
	
	// login/logout changes presence so tell everyone sharing a room
	private void announcePresence(UUID user) {
		if (user != null) {
			dispatcher.sendToPeers(user, RealtimeEventType.USER_UPDATED, Sputnik.user(userService.getPrivateUser(user)));
		}
	}
}
