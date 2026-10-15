package dev.ktpm;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.ktpm.api.AssetController;
import dev.ktpm.api.AuthController;
import dev.ktpm.api.ChatMessageController;
import dev.ktpm.api.ChatRoomController;
import dev.ktpm.api.EventController;
import dev.ktpm.api.SSEventDispatcher;
import dev.ktpm.api.RoomMemberController;
import dev.ktpm.api.UserController;
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
		// uploads live on the filesystem: usercontent/<id>.<ext>, serveravatars/<room uuid>.<ext>
		var assetService = new AssetService(assets, Path.of("usercontent"), Path.of("serveravatars"));
		var userService = new UserService(users, assetService);
		var roomService = new ChatRoomService(rooms, memberships, users, messages, assetService);
		var memberService = new RoomMemberService(rooms, memberships, users);
		var messageService = new ChatMessageService(messages, memberships, rooms, assetService);
		var hub = new SSEventDispatcher(memberships);
		
		var j = Javalin.create(config -> {
			config.jsonMapper(new JavalinJackson(new ObjectMapper(), false));
			config.http.maxRequestSize = 40L * 1024 * 1024; // attachments come in as base64, so ~33% bigger than the file
			config.staticFiles.add("/public", Location.CLASSPATH, java.util.Set.of());
			config.routes.get("/api/health", ctx -> ctx.result("OK"));
			new AuthController(authService, userService, hub).registerRoutes(config.routes);
			new UserController(userService, authService, hub).registerRoutes(config.routes);
			new AssetController(assetService, authService).registerRoutes(config.routes);
			new ChatRoomController(roomService, authService, userService, assetService, hub).registerRoutes(config.routes);
			new RoomMemberController(memberService, authService, userService, hub).registerRoutes(config.routes);
			new ChatMessageController(messageService, authService, userService, hub).registerRoutes(config.routes);
			new EventController(authService, hub).registerRoutes(config.routes);
		});
		java.lang.Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			j.stop();
			hibernate.close();
		}, "application-shutdown"));
		j.start(8080);
	}
}
