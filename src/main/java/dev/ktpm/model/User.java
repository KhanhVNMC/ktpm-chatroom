package dev.ktpm.model;

import java.util.UUID;

public record User(UUID uuid, String username, String displayName) {}
