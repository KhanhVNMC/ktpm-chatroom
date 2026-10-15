package dev.ktpm.api.dto;

import java.util.List;
import java.util.Optional;

public record SendMessageRequest(String messageContent, Optional<List<UploadedFile>> uploadedFiles) {}
