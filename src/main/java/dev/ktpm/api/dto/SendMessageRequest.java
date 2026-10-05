package dev.ktpm.api.dto;

import dev.ktpm.model.AttachmentAsset;

import java.util.List;
import java.util.Optional;

public record SendMessageRequest(String messageContent, Optional<List<AttachmentAsset>> uploadedFiles) {}
