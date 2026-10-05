package dev.ktpm.api.dto;

import java.util.Optional;

public record JoinRoomRequest(Optional<String> roomInviteCode) {}
