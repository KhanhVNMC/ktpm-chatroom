package dev.ktpm.service;

import dev.ktpm.api.dto.UpdateProfileRequest;
import dev.ktpm.model.User;

import java.util.UUID;

public interface IUserService {
	User getPublicUser(UUID requester, UUID target);

	User getPrivateUser(UUID requester);

	// change display name / avatar / presence / status text, returns the updated user
	User updateProfile(UUID requester, UpdateProfileRequest request);
}
