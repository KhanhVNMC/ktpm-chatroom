package dev.ktpm;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.ktpm.api.AuthController;
import dev.ktpm.api.ChatMessageController;
import dev.ktpm.api.ChatRoomController;
import dev.ktpm.api.UserController;
import dev.ktpm.impl.jpa.HibernateRuntime;
import dev.ktpm.impl.jpa.repos.JPAChatMessageRepository;
import dev.ktpm.impl.jpa.repos.JPAChatRoomRepository;
import dev.ktpm.impl.jpa.repos.JPARoomMembershipRepository;
import dev.ktpm.impl.jpa.repos.JPASessionRepository;
import dev.ktpm.impl.jpa.repos.JPAUserCredentialsRepository;
import dev.ktpm.impl.jpa.repos.JPAUserRepository;
import dev.ktpm.impl.service.AuthenticationService;
import dev.ktpm.impl.service.ChatMessageService;
import dev.ktpm.impl.service.ChatRoomService;
import dev.ktpm.impl.service.UserService;
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
		
		var authService = new AuthenticationService(users, credentials, sessions);
		var userService = new UserService(users);
		var roomService = new ChatRoomService(rooms, memberships, users);
		var messageService = new ChatMessageService(messages, memberships, rooms);
		
		var j = Javalin.create(config -> {
			config.jsonMapper(new JavalinJackson(new ObjectMapper(), false));
			config.staticFiles.add("/public", Location.CLASSPATH, java.util.Set.of());
			config.routes.get("/api/health", ctx -> ctx.result("OK"));
			new AuthController(authService, userService).registerRoutes(config.routes);
			new UserController(userService, authService).registerRoutes(config.routes);
			new ChatRoomController(roomService, authService).registerRoutes(config.routes);
			new ChatMessageController(messageService, authService, userService).registerRoutes(config.routes);
		});
		java.lang.Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			j.stop();
			hibernate.close();
		}, "application-shutdown"));
		j.start(8080);
	}
}
