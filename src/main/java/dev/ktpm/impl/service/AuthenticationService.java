package dev.ktpm.impl.service;

import dev.ktpm.service.IAuthenticationService;
import dev.ktpm.model.Session;
import dev.ktpm.model.User;
import dev.ktpm.model.UserCredentials;
import dev.ktpm.repository.ISessionRepository;
import dev.ktpm.repository.IUserCredentialsRepository;
import dev.ktpm.repository.IUserRepository;
import org.mindrot.jbcrypt.BCrypt;
import java.util.Optional;
import java.util.UUID;

public class AuthenticationService implements IAuthenticationService {

    private final IUserRepository userRepository;
    private final IUserCredentialsRepository credentialsRepository;
    private final ISessionRepository sessionRepository;

    public AuthenticationService(IUserRepository userRepository, IUserCredentialsRepository credentialsRepository, ISessionRepository sessionRepository) {
        this.userRepository = userRepository;
        this.credentialsRepository = credentialsRepository;
        this.sessionRepository = sessionRepository;
    }

    @Override
    public String login(String username, String password) {
        Optional<User> requestedUser = userRepository.getByUsername(username);
        if (requestedUser.isEmpty()) {
            return null; 
        }
        UUID userUuid = requestedUser.get().uuid();

        Optional<UserCredentials> creds = credentialsRepository.get(userUuid);
        if (creds.isEmpty()) {
            return null;
        }

        String storedHash = creds.get().passwordHash();
        if (!BCrypt.checkpw(password, storedHash)) {
            return null;
        }

        Session newSession = sessionRepository.create(userUuid);
        return newSession.sessionID();
    }

    @Override
    public UUID authenticate(String sessionID) {
        Optional<Session> requestedSession = sessionRepository.get(sessionID);
        if (requestedSession.isEmpty()) {
            return null;
        }
        Session session = requestedSession.get();
        return session.userUUID();
    }

    @Override
    public void logout(String sessionID) {
        sessionRepository.delete(sessionID);
    }

    @Override
    public User register(String username, String displayName, String password) {
        if (userRepository.getByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());
        User newUser = userRepository.create(new User(UUID.randomUUID(), username, displayName));
        UserCredentials newCredentials = new UserCredentials(newUser.uuid(), passwordHash);
        credentialsRepository.set(newCredentials);
        return newUser;
    }
}
