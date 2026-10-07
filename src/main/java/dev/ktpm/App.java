package dev.ktpm;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ktpm.impl.jpa.HibernateRuntime;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

public class App {
	public static void main(String[] args) {
		HibernateRuntime hibernate = HibernateRuntime.bootstrap();
		var j = Javalin.create(config -> {
			config.jsonMapper(new JavalinJackson(new ObjectMapper(), false));
			config.routes.get("/api/health", ctx -> ctx.result("OK"));
		});
		java.lang.Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			j.stop();
			hibernate.close();
		}, "application-shutdown"));
		j.start(8080);
	}
}
