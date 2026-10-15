package dev.ktpm.api;

import java.util.UUID;

import dev.ktpm.service.IAuthenticationService;
import io.javalin.http.Context;
import io.javalin.http.sse.SseHandler;
import io.javalin.router.JavalinDefaultRoutingApi;

public class EventController {
	private final IAuthenticationService authService;
	private final SSEventDispatcher dispatcher;
	
	public EventController(IAuthenticationService authService, SSEventDispatcher dp) {
		this.authService = authService;
		this.dispatcher = dp;
	}
	
	public void registerRoutes(JavalinDefaultRoutingApi router) {
		router.get("/api/events", Sputnik.handle(this::connect));
	}
	
	public void connect(Context ctx) {
		String token = Sputnik.token(ctx);
		if (token == null) {
			token = ctx.queryParam("session_id");
		}
		UUID user = token == null ? null : authService.authenticate(token);
		if (user == null) {
			throw new APIException(401, "Authentication required");
		}
		String accept = ctx.header("Accept");
		if (accept == null || !accept.contains("text/event-stream")) {
			throw new APIException(406, "Accept: text/event-stream is required");
		}
		
		String session = token;
		new SseHandler(0L, client -> dispatcher.connect(user, session, client)).handle(ctx);
	}
}
