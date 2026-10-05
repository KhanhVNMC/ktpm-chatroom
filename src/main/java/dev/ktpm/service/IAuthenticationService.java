package dev.ktpm.service;

import java.util.UUID;

public interface IAuthenticationService {
	String login(String username, String password);
	
	UUID authenticate(String sessionID);
	
	void logout(String sessionID);
}
