package dev.ktpm.service;

import dev.ktpm.api.dto.UpdateProfileRequest;
import dev.ktpm.model.User;

import java.util.UUID;

public interface IUserService {
	User getUser(UUID target);
	
	// change display name / presence / status text, returns the updated user
	User updateProfile(UUID requester, UpdateProfileRequest request);
	
	// images only, saved to useravatars/<user uuid>.<ext>
	User setAvatar(UUID requester, byte[] data, String originalName);
	
	User removeAvatar(UUID requester);
}
