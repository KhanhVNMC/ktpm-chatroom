package dev.ktpm.api;

import java.util.UUID;

import io.javalin.http.Context;
import io.javalin.http.sse.SseHandler;
import io.javalin.router.JavalinDefaultRoutingApi;

public class EventController {
	private final SSEventDispatcher dispatcher;
	
	public EventController(SSEventDispatcher dp) {
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
		//Middleware sẽ xử lý authentication trước đó
		UUID user = Sputnik.authenticatedUser(ctx);
		
		String accept = ctx.header("Accept");
		if (accept == null || !accept.contains("text/event-stream")) {
			throw new APIException(406, "Accept: text/event-stream is required");
		}
		
		String session = token;
		new SseHandler(0L, client -> dispatcher.connect(user, session, client)).handle(ctx);
	}
}
