package dev.ktpm.repository;

import dev.ktpm.model.AttachmentAsset;

import java.util.Optional;
import java.util.UUID;

public interface IAttachmentAssetRepository {
	Optional<AttachmentAsset> get(UUID assetUUID);

	AttachmentAsset create(AttachmentAsset asset);

	void delete(UUID assetUUID);
}
