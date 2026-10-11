package dev.ktpm.impl.service;

import java.util.UUID;
import java.util.Optional;

import dev.ktpm.model.User;
import dev.ktpm.repository.IUserRepository;
import dev.ktpm.service.IUserService;

public class UserService implements IUserService {
    private final IUserRepository userRepo;

    public UserService(IUserRepository userRepo) {
        this.userRepo = userRepo;
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
}
