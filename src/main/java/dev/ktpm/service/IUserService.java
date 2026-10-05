package dev.ktpm.service;

import dev.ktpm.model.User;

import java.util.UUID;

public interface IUserService {
	User getPublicUser(UUID requester, UUID target);
	
	User getPrivateUser(UUID requester);
}
