package dev.ktpm.api.dto;

import java.util.UUID;

public record RoomListingResponse(
	UUID roomUUID,
	String motd,
	int members
) {}
