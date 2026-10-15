package dev.ktpm.impl.jpa.entities;

import dev.ktpm.model.AttachmentAsset;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.nio.file.Path;
import java.util.UUID;

@Entity
@Table(name = "attachment_assets")
@Getter @Setter
@NoArgsConstructor
public class AttachmentAssetEntity {
	@Id
	@Column(name = "uuid")
	private UUID id;
	
	@Column(name = "file_path", nullable = false)
	private String path;
	
	@Column(name = "original_name")
	private String originalName;
	
	public static AttachmentAssetEntity fromModel(AttachmentAsset model) {
		var entity = new AttachmentAssetEntity();
		entity.id = model.id();
		entity.path = model.path().toString();
		entity.originalName = model.originalName();
		return entity;
	}
	
	public AttachmentAsset toModel() {
		return new AttachmentAsset(id, Path.of(path), originalName);
	}
}
