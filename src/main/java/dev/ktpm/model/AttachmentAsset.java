package dev.ktpm.model;

import java.nio.file.Path;
import java.util.UUID;

// SSP (server side path): the real file lives on the filesystem, db only keeps the path + original name
public record AttachmentAsset(UUID id, Path path, String originalName) {}
