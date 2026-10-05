package dev.ktpm.model;

import java.util.UUID;

public record UserCredentials(UUID userUUID, String passwordHash) {}
