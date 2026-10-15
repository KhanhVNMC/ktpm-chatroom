package dev.ktpm.api;

import java.util.UUID;

import dev.ktpm.service.IAssetService;
import dev.ktpm.service.IAuthenticationService;
import io.javalin.http.Context;
import io.javalin.router.JavalinDefaultRoutingApi;

public class AssetController {
	private final IAssetService assetService;
	private final IAuthenticationService authService;

	public AssetController(IAssetService assetService, IAuthenticationService authService) {
		this.assetService = assetService;
		this.authService = authService;
	}

	public void registerRoutes(JavalinDefaultRoutingApi router) {
		router.post("/api/assets", Sputnik.handle(this::upload));
		router.get("/api/assets/{id}", Sputnik.handle(this::download));
	}

	// raw body (+ X-File-Name header) or multipart field "file" -> {asset_uuid, url}. used for user avatars
	public void upload(Context ctx) {
		Sputnik.requireAuth(ctx, authService);
		var file = Sputnik.upload(ctx);
		var asset = assetService.upload(file.data(), file.originalName());
		ctx.status(201);
		ctx.json(Sputnik.obj(
			"asset_uuid", asset.id(), "url", Sputnik.assetUrl(asset.id()), "name", asset.originalName()
		));
	}

	// no auth, <img>/<video> cant send headers
	public void download(Context ctx) {
		var asset = assetService.get(UUID.fromString(ctx.pathParam("id")))
			.orElseThrow(() -> new IllegalArgumentException("Asset not found"));
		Sputnik.sendFile(ctx, asset.path(), assetService.contentType(asset.path()));
	}
}
