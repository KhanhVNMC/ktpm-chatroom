package dev.ktpm.model;

import java.util.UUID;

public record RoomMembership(
	UUID roomUUID, UUID userUUID, long joinedAt, 
	int permissionBits, int statusBits
) {}
