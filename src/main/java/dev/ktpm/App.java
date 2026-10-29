package dev.ktpm;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.ktpm.api.*;
import dev.ktpm.impl.jpa.HibernateRuntime;
import dev.ktpm.impl.jpa.repos.JPAAttachmentAssetRepository;
import dev.ktpm.impl.jpa.repos.JPAChatMessageRepository;
import dev.ktpm.impl.jpa.repos.JPAChatRoomRepository;
import dev.ktpm.impl.jpa.repos.JPARoomMembershipRepository;
import dev.ktpm.impl.jpa.repos.JPASessionRepository;
import dev.ktpm.impl.jpa.repos.JPAUserCredentialsRepository;
import dev.ktpm.impl.jpa.repos.JPAUserRepository;
import dev.ktpm.impl.service.AssetService;
import dev.ktpm.impl.service.AuthenticationService;
import dev.ktpm.impl.service.ChatMessageService;
import dev.ktpm.impl.service.ChatRoomService;
import dev.ktpm.impl.service.RoomMemberService;
import dev.ktpm.impl.service.UserService;
import java.nio.file.Path;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.json.JavalinJackson;

public class App {
	public static void main(String[] args) {
		HibernateRuntime hibernate = HibernateRuntime.bootstrap();
		
		var users = new JPAUserRepository(hibernate);
		var credentials = new JPAUserCredentialsRepository(hibernate);
		var sessions = new JPASessionRepository(hibernate);
		var rooms = new JPAChatRoomRepository(hibernate);
		var memberships = new JPARoomMembershipRepository(hibernate);
		var messages = new JPAChatMessageRepository(hibernate);
		var assets = new JPAAttachmentAssetRepository(hibernate);
		
		var authService = new AuthenticationService(users, credentials, sessions);
		// uploads live on the server fs usercontent, serveravatars and useravatars
		var assetService = new AssetService(assets, Path.of("usercontent"), Path.of("serveravatars"), Path.of("useravatars"));
		var userService = new UserService(users, assetService);
		var roomService = new ChatRoomService(rooms, memberships, users, messages, assetService);
		var memberService = new RoomMemberService(rooms, memberships, users);
		var messageService = new ChatMessageService(messages, memberships, rooms, assetService);
		var hub = new SSEventDispatcher(memberships);
		
		var j = Javalin.create(config -> {
			config.jsonMapper(new JavalinJackson(new ObjectMapper(), false));
			config.http.maxRequestSize = 40L * 1024 * 1024;
			config.staticFiles.add("/public", Location.CLASSPATH);
			
			config.routes.before(
				"/api/*",
				new AuthenticationMiddleware(authService).handler()
			);
			
			// wraps all exceptions in a neat lil bwotie so the api dont
			// expose the entire server stack trace 4 the world to look at
			config.routes.exception(APIException.class, (exception, ctx) -> {
				ctx.status(exception.status);
				ctx.json(Sputnik.obj(
					"error_message", exception.getMessage()
				));
			});
			
			config.routes.get("/api/health", ctx -> ctx.result("OK"));
			
			new AuthController(authService, userService, hub).registerRoutes(config.routes);
			new UserController(userService, assetService, hub).registerRoutes(config.routes);
			new AssetController(assetService).registerRoutes(config.routes);
			new ChatRoomController(
				roomService, userService, assetService, hub
			).registerRoutes(config.routes);
			new RoomMemberController(
				memberService, userService, hub
			).registerRoutes(config.routes);
			new ChatMessageController(
				messageService, userService, hub
			).registerRoutes(config.routes);
			new EventController(hub).registerRoutes(config.routes);
		});
		
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			j.stop();
			hibernate.close();
		}, "application-shutdown"));
		
		j.start(8080);
	}
}
