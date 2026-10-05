package dev.ktpm;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

public class App {
	public static void main(String[] args) {
		var j = Javalin.create(config -> {
			config.jsonMapper(new JavalinJackson(new ObjectMapper(), false));
			config.routes.get("/api/health", ctx -> ctx.result("OK"));
		});
		j.start(8080);
	}
}
