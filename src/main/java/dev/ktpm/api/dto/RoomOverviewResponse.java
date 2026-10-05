package dev.ktpm.api.dto;

import java.util.List;

public record RoomOverviewResponse(
	String motd,
	String owner,
	List<String> memberNames
) {}
