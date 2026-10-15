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
    public User getPublicUser(UUID requester, UUID target) {
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
    public User getPrivateUser(UUID requester) {
        if (requester == null) {
            throw new IllegalArgumentException("Requester user UUID must not be null");
        }

        Optional<User> userOpt = userRepo.get(requester);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }

        return userOpt.get();
    }

    @Override
    public User updateProfile(UUID requester, UpdateProfileRequest request) {
        User user = getPrivateUser(requester);

        String displayName = user.displayName();
        if (request.displayName().isPresent()) {
            displayName = request.displayName().get().trim();
            if (displayName.isEmpty() || displayName.length() > 64) {
                throw new IllegalArgumentException("Display name must be 1-64 characters");
            }
        }

        UUID avatar = user.avatar();
        if (request.clearAvatar()) {
            avatar = null;
        } else if (request.avatar().isPresent()) {
            avatar = assetService.requireImage(request.avatar().get());
        }

        String statusText = user.statusText();
        if (request.statusText().isPresent()) {
            statusText = request.statusText().get().trim();
            if (statusText.length() > 128) {
                throw new IllegalArgumentException("Status text must be at most 128 characters");
            }
            statusText = statusText.isEmpty() ? null : statusText;
        }

        User updated = userRepo.update(new User(
            user.uuid(), user.username(), displayName, avatar,
            request.presence().orElse(user.presence()), statusText
        ));

        // old avatar isnt used by anyone anymore, clean it up
        if (user.avatar() != null && !user.avatar().equals(avatar)) {
            assetService.delete(user.avatar());
        }
        return updated;
    }
}
