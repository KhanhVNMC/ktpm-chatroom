package dev.ktpm.service;

import java.util.UUID;
import dev.ktpm.model.*;

public interface IAuthenticationService {
	String login(String username, String password);
	
	UUID authenticate(String sessionID);
	
	void logout(String sessionID);

	User register(String username, String displayName, String password);
}
