package dev.ktpm.impl.jpa.repos;

import java.util.Optional;
import java.util.UUID;

import dev.ktpm.impl.jpa.HibernateRuntime;
import dev.ktpm.impl.jpa.entities.AttachmentAssetEntity;
import dev.ktpm.model.AttachmentAsset;
import dev.ktpm.repository.IAttachmentAssetRepository;

public class JPAAttachmentAssetRepository implements IAttachmentAssetRepository {
	private final HibernateRuntime runtime;
	
	public JPAAttachmentAssetRepository(HibernateRuntime runtime) {
		this.runtime = runtime;
	}
	
	@Override
	public Optional<AttachmentAsset> get(UUID assetUUID) {
		try (var man = runtime.openEntityManager()) {
			AttachmentAssetEntity entity = man.find(AttachmentAssetEntity.class, assetUUID);
			return entity == null ? Optional.empty() : Optional.of(entity.toModel());
		}
	}
	
	@Override
	public AttachmentAsset create(AttachmentAsset asset) {
		try (var man = runtime.open()) {
			var entity = AttachmentAssetEntity.fromModel(asset);
			man.eman.persist(entity);
			man.eman.flush();
			return entity.toModel();
		}
	}
	
	@Override
	public void delete(UUID assetUUID) {
		try (var man = runtime.open()) {
			AttachmentAssetEntity entity = man.eman.find(AttachmentAssetEntity.class, assetUUID);
			if (entity != null) {
				man.eman.remove(entity);
			}
		}
	}
}
