package dev.ktpm.model;

import java.util.UUID;

public record ChatRoom(UUID uuid, UUID owner, String motd, boolean isPublic, String inviteCode) {}
