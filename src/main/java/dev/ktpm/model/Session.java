package dev.ktpm.model;

import java.util.UUID;

public record Session(String sessionID, UUID userUUID, long createdAt, long expiresAt) {}
