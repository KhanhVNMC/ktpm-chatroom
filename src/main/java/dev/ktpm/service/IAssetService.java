package dev.ktpm.service;

import dev.ktpm.model.AttachmentAsset;

import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface IAssetService {
	static URI uri(UUID assetUUID) {
		return URI.create("/api/assets/" + assetUUID);
	}
	
	// write the file to usercontent/<id>.<original ext>, returns the saved asset
	AttachmentAsset upload(byte[] data, String originalName);
	
	Optional<AttachmentAsset> get(UUID assetUUID);
	
	void delete(UUID assetUUID);
	
	// delete the assets the URIs point to (anything not /api/assets/... is ignored)
	void deleteAll(Collection<URI> uris);
	
	// write room avatar to serveravatars/<room uuid>.<ext>, replaces the old one. images only
	Path saveRoomAvatar(UUID roomUUID, byte[] data, String originalName);
	
	// write user avatar to useravatars/<user uuid>.<ext>, replaces the old one. images only
	Path saveUserAvatar(UUID userUUID, byte[] data, String originalName);
	
	// works for both room and user avatars
	void deleteAvatar(Path avatar);
	
	// content type guessed from the extension
	String contentType(Path file);
}
