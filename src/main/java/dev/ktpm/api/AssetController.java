package dev.ktpm.api;

import java.util.UUID;

import dev.ktpm.service.IAssetService;
import io.javalin.http.Context;
import io.javalin.router.JavalinDefaultRoutingApi;

public class AssetController {
	private final IAssetService assetService;
	
	public AssetController(IAssetService assetService) {
		this.assetService = assetService;
	}
	
	public void registerRoutes(JavalinDefaultRoutingApi router) {
		router.get("/api/assets/{id}", Sputnik.handle(this::download));
	}
	
	// no auth, <img>/<video> cant send headers
	public void download(Context ctx) {
		var asset = assetService.get(UUID.fromString(ctx.pathParam("id")))
			.orElseThrow(() -> new IllegalArgumentException("Asset not found"))
		;
		Sputnik.sendFile(ctx, asset.path(), assetService.contentType(asset.path()));
	}
}
