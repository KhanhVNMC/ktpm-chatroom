package dev.ktpm.impl.jpa.entities;

import dev.ktpm.model.AttachmentAsset;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "attachment_assets")
@Getter @Setter
@NoArgsConstructor
public class AttachmentAssetEntity {
	@Id
	@Column(name = "uuid")
	private UUID uuid;
	
	@Lob
	@Column(name = "attachment_data")
	private byte attachment[];
	
	@Column(name = "size", nullable = false)
	private long size;
	
	public static AttachmentAssetEntity fromModel(AttachmentAsset model) {
		var entity = new AttachmentAssetEntity();
		entity.uuid = model.uuid();
		entity.attachment = model.attachment();
		entity.size = model.size();
		return entity;
	}
	
	public AttachmentAsset toModel() {
		return new AttachmentAsset(uuid, attachment, size);
	}
}
