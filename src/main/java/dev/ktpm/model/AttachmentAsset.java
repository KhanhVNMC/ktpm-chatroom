package dev.ktpm.model;

import java.util.UUID;

public record AttachmentAsset(UUID uuid, byte[] attachment, long size) {}
