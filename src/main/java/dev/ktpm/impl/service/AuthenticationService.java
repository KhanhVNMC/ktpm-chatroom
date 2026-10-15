package dev.ktpm.impl.service;

import dev.ktpm.consts.UserPresence;
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
        if (username == null || password == null) {
            return null;
        }
        Optional<User> requestedUser = userRepository.getByUsername(username.trim().toLowerCase());
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

        setPresence(requestedUser.get(), UserPresence.ONLINE);
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
        if (session.expiresAt() <= System.currentTimeMillis()) {
            sessionRepository.delete(sessionID);
            return null;
        }
        return session.userUUID();
    }

    @Override
    public void logout(String sessionID) {
        Optional<Session> session = sessionRepository.get(sessionID);
        if (session.isEmpty()) {
            return;
        }
        sessionRepository.delete(sessionID);
        userRepository.get(session.get().userUUID()).ifPresent(user -> setPresence(user, UserPresence.OFFLINE));
    }

    private void setPresence(User user, UserPresence presence) {
        userRepository.update(new User(
            user.uuid(), user.username(), user.displayName(), user.avatar(), presence, user.statusText()
        ));
    }

    @Override
    public User register(String username, String displayName, String password) {
        if (username == null || !username.trim().matches("[A-Za-z0-9_.]{3,32}")) {
            throw new IllegalArgumentException("Username must be 3-32 characters (letters, digits, _ or .)");
        }
        if (password == null || password.length() < 6 || password.length() > 72) { // bcrypt only reads 72 bytes at most
            throw new IllegalArgumentException("Password must be 6-72 characters");
        }
        if (displayName == null || displayName.isBlank() || displayName.trim().length() > 64) {
            throw new IllegalArgumentException("Display name must be 1-64 characters");
        }
        // usernames are stored lowercase so "Alice" and "alice" arent 2 accounts
        String normalized = username.trim().toLowerCase();
        if (userRepository.getByUsername(normalized).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());
        User newUser = userRepository.create(new User(
            UUID.randomUUID(), normalized, displayName.trim(), null, UserPresence.OFFLINE, null
        ));
        UserCredentials newCredentials = new UserCredentials(newUser.uuid(), passwordHash);
        credentialsRepository.set(newCredentials);
        return newUser;
    }
}
