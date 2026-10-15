package dev.ktpm.impl.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import dev.ktpm.model.AttachmentAsset;
import dev.ktpm.repository.IAttachmentAssetRepository;
import dev.ktpm.service.IAssetService;

public class AssetService implements IAssetService {
	public static final long MAX_SIZE = 25L * 1024 * 1024;
	private static final String PREFIX = "/api/assets/";

	// extension -> content type (anything not here gets downloaded as octet-stream)
	private static final Map<String, String> TYPES = Map.ofEntries(
		Map.entry("png", "image/png"), Map.entry("jpg", "image/jpeg"), Map.entry("jpeg", "image/jpeg"),
		Map.entry("gif", "image/gif"), Map.entry("webp", "image/webp"),
		Map.entry("mp4", "video/mp4"), Map.entry("webm", "video/webm"), Map.entry("mov", "video/quicktime"),
		Map.entry("mp3", "audio/mpeg"), Map.entry("wav", "audio/wav"), Map.entry("ogg", "audio/ogg"),
		Map.entry("m4a", "audio/mp4"), Map.entry("opus", "audio/opus"), Map.entry("pdf", "application/pdf"),
		Map.entry("txt", "text/plain")
	);
	private static final java.util.Set<String> IMAGES = java.util.Set.of("png", "jpg", "jpeg", "gif", "webp");

	private final IAttachmentAssetRepository assetRepo;
	private final Path userContentDir;
	private final Path roomAvatarDir;

	public AssetService(IAttachmentAssetRepository assetRepo, Path userContentDir, Path roomAvatarDir) {
		this.assetRepo = assetRepo;
		this.userContentDir = userContentDir;
		this.roomAvatarDir = roomAvatarDir;
	}

	@Override
	public AttachmentAsset upload(byte[] data, String originalName) {
		checkSize(data);
		UUID id = UUID.randomUUID();
		Path file = userContentDir.resolve(id + "." + extension(originalName));
		write(file, data);
		try {
			return assetRepo.create(new AttachmentAsset(id, file, safeName(originalName)));
		} catch (RuntimeException e) {
			deleteFile(file); // db failed, dont leave an orphan file
			throw e;
		}
	}

	@Override
	public Optional<AttachmentAsset> get(UUID assetUUID) {
		return assetRepo.get(assetUUID);
	}

	@Override
	public UUID requireImage(UUID assetUUID) {
		AttachmentAsset asset = assetRepo.get(assetUUID)
			.orElseThrow(() -> new IllegalArgumentException("Asset not found"));
		if (!IMAGES.contains(extensionOf(asset.path()))) {
			throw new IllegalArgumentException("Asset is not an image");
		}
		return assetUUID;
	}

	@Override
	public void delete(UUID assetUUID) {
		assetRepo.get(assetUUID).ifPresent(asset -> {
			assetRepo.delete(assetUUID);
			deleteFile(asset.path());
		});
	}

	@Override
	public void deleteAll(Collection<URI> uris) {
		for (URI uri : uris) {
			String path = uri.toString();
			if (!path.startsWith(PREFIX)) {
				continue;
			}
			try {
				delete(UUID.fromString(path.substring(PREFIX.length())));
			} catch (IllegalArgumentException e) {
				// weird url, whatever
			}
		}
	}

	@Override
	public Path saveRoomAvatar(UUID roomUUID, byte[] data, String originalName) {
		checkSize(data);
		String ext = extension(originalName);
		if (!IMAGES.contains(ext)) {
			throw new IllegalArgumentException("Avatar must be an image (png, jpg, gif, webp)");
		}
		Path file = roomAvatarDir.resolve(roomUUID + "." + ext);
		// extension changed (png -> jpg), old file has to go
		for (String other : IMAGES) {
			deleteFile(roomAvatarDir.resolve(roomUUID + "." + other));
		}
		write(file, data);
		return file;
	}

	@Override
	public void deleteRoomAvatar(Path avatar) {
		if (avatar != null) {
			deleteFile(avatar);
		}
	}

	@Override
	public String contentType(Path file) {
		return TYPES.getOrDefault(extensionOf(file), "application/octet-stream");
	}

	private static void checkSize(byte[] data) {
		if (data == null || data.length == 0) {
			throw new IllegalArgumentException("File must not be empty");
		}
		if (data.length > MAX_SIZE) {
			throw new IllegalArgumentException("File is too large (max " + (MAX_SIZE / 1024 / 1024) + " MB)");
		}
	}

	// extension must be [a-z0-9]{1,8} else "bin" (stops path traversal through the filename)
	private static String extension(String originalName) {
		if (originalName == null) {
			return "bin";
		}
		int dot = originalName.lastIndexOf('.');
		String ext = dot < 0 ? "" : originalName.substring(dot + 1).toLowerCase();
		return ext.matches("[a-z0-9]{1,8}") ? ext : "bin";
	}

	private static String extensionOf(Path file) {
		return extension(file.getFileName().toString());
	}

	// keep the file name only, drop any directories
	private static String safeName(String originalName) {
		if (originalName == null || originalName.isBlank()) {
			return "file";
		}
		String name = originalName.replace('\\', '/');
		name = name.substring(name.lastIndexOf('/') + 1).trim();
		return name.isEmpty() ? "file" : name.substring(0, Math.min(name.length(), 255));
	}

	private static void write(Path file, byte[] data) {
		try {
			Files.createDirectories(file.getParent());
			Files.write(file, data);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static void deleteFile(Path file) {
		try {
			Files.deleteIfExists(file);
		} catch (IOException e) {
			// cant delete it? fine, dont break the request over it
		}
	}
}
