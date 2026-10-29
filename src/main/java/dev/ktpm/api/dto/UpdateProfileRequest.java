package dev.ktpm.api.dto;

import java.util.Optional;

import dev.ktpm.consts.UserPresence;

// missing field (Optional.empty) = keep as is
// avatar has its own endpoint now
public record UpdateProfileRequest(
	Optional<String> displayName, 
	Optional<UserPresence> presence,
	Optional<String> statusText
) {}
