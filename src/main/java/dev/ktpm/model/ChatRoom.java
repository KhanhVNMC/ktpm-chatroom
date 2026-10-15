package dev.ktpm.model;

import java.nio.file.Path;
import java.util.UUID;

public record ChatRoom(
	UUID uuid, UUID owner, String name, String motd, 
	Path avatar, long createdAt, boolean isPublic, String inviteCode
) {}
