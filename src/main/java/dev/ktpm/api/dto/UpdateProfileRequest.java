package dev.ktpm.api.dto;

import java.util.Optional;
import java.util.UUID;

import dev.ktpm.consts.UserPresence;

// missing field (Optional.empty) = keep as is
// clearAvatar = true -> removes the avatar (avatar is ignored then)
public record UpdateProfileRequest(
	Optional<String> displayName, 
	Optional<UUID> avatar, 
	boolean clearAvatar,
	Optional<UserPresence> presence,
	Optional<String> statusText
) {}
