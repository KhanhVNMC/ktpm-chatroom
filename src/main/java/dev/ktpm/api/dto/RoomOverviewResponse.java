package dev.ktpm.api.dto;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import dev.ktpm.model.User;

// inviteCode is only set when the viewer has MANAGE_ROOM (null otherwise)
public record RoomOverviewResponse(
	UUID roomUUID,
	String name,
	String motd,
	Path avatar,
	long createdAt,
	boolean isPublic,
	String owner,
	UUID ownerUUID,
	int memberCount,
	List<User> members,
	String inviteCode
) {}
