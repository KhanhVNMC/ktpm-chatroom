package dev.ktpm.model;

import java.util.UUID;

import dev.ktpm.consts.UserPresence;

// avatar = asset uuid (null = none), statusText = custom status line (null = empty)
public record User(
	UUID uuid, String username, String displayName,
	UUID avatar, UserPresence presence, String statusText
) {}
