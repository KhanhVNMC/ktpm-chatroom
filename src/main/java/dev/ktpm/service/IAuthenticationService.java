package dev.ktpm.service;

import java.util.UUID;
import dev.ktpm.model.*;

public interface IAuthenticationService {
	// username is case insensitive. presence = ONLINE after login
	String login(String username, String password);
	
	// null if the session doesnt exist or expired
	UUID authenticate(String sessionID);
	
	// kill the session, presence = OFFLINE
	void logout(String sessionID);
	
	User register(String username, String displayName, String password);
}
