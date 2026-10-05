package dev.ktpm.api.dto;

import java.util.Optional;

public record LeaveRoomRequest(boolean silentLeave, Optional<String> reason) {}
