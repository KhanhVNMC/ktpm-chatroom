package dev.ktpm.model;

import java.nio.file.Path;
import java.util.UUID;

import dev.ktpm.consts.UserPresence;

// avatar = path of the avatar file (useravatars/<user uuid>.<ext>, null = none), statusText = custom status line (null = empty)
public record User(
	UUID uuid, String username, String displayName,
	Path avatar, UserPresence presence, String statusText
) {}
