package dev.ktpm.impl.service;

import java.util.UUID;
import java.util.Optional;

import dev.ktpm.api.dto.UpdateProfileRequest;
import dev.ktpm.model.User;
import dev.ktpm.repository.IUserRepository;
import dev.ktpm.service.IAssetService;
import dev.ktpm.service.IUserService;

public class UserService implements IUserService {
	private final IUserRepository userRepo;
	private final IAssetService assetService;
	
	public UserService(IUserRepository userRepo, IAssetService assetService) {
		this.userRepo = userRepo;
		this.assetService = assetService;
	}
	
	@Override
	public User getUser(UUID target) {
		if (target == null) {
			throw new IllegalArgumentException("Target user UUID must not be null");
		}
		Optional<User> userOpt = userRepo.get(target);
		if (userOpt.isEmpty()) {
			throw new IllegalArgumentException("User not found");
		}
		return userOpt.get();
	}
	
	@Override
	public User updateProfile(UUID requester, UpdateProfileRequest request) {
		User user = this.getUser(requester);
		String displayName = user.displayName();
		if (request.displayName().isPresent()) {
			displayName = request.displayName().get().trim();
			if (displayName.isEmpty() || displayName.length() > 64) {
				throw new IllegalArgumentException("Display name must be 1-64 characters");
			}
		}
		String statusText = user.statusText();
		if (request.statusText().isPresent()) {
			statusText = request.statusText().get().trim();
			if (statusText.length() > 128) {
				throw new IllegalArgumentException("Status text must be at most 128 characters");
			}
			statusText = statusText.isEmpty() ? null : statusText;
		}
		return userRepo.update(new User(
			user.uuid(), user.username(), displayName, user.avatar(),
			request.presence().orElse(user.presence()), statusText)
		);
	}
	
	@Override
	public User setAvatar(UUID requester, byte[] data, String originalName) {
		User user = this.getUser(requester);
		var avatar = assetService.saveUserAvatar(user.uuid(), data, originalName);
		return userRepo.update(new User(
			user.uuid(), user.username(), 
			user.displayName(), avatar,
			user.presence(), user.statusText()
		));
	}
	
	@Override
	public User removeAvatar(UUID requester) {
		User user = this.getUser(requester);
		assetService.deleteAvatar(user.avatar());
		return userRepo.update(new User(
			user.uuid(), user.username(), 
			user.displayName(), null,
			user.presence(), user.statusText()
		));
	}
}
