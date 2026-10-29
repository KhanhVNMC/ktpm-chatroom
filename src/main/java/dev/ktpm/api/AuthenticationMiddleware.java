package dev.ktpm.api;

import java.util.UUID;

import dev.ktpm.service.IAuthenticationService;
import io.javalin.http.Context;
import io.javalin.http.Handler;

public final class AuthenticationMiddleware {
    public static final String AUTHENTICATED_USER = "authenticated_user";

    private final IAuthenticationService authService;

    public AuthenticationMiddleware(IAuthenticationService authService) {
        this.authService = authService;
    }

    public Handler handler() {
        return this::authenticate;
    }

    private void authenticate(Context ctx) {
        if (!requiresAuthentication(ctx)) {
            authenticateIfProvided(ctx);
            return;
        }

        String token = Sputnik.token(ctx);

        // SSE không gửi được Authorization header nên cho phép query parameter.
        if (token == null && ctx.path().equals("/api/events")) {
            token = ctx.queryParam("session_id");
        }

        if (token == null || token.isBlank()) {
            throw new APIException(401, "Authentication required");
        }

        UUID user = authService.authenticate(token);
        if (user == null) {
            throw new APIException(401, "Invalid or expired session");
        }

        ctx.attribute(AUTHENTICATED_USER, user);
    }

    private void authenticateIfProvided(Context ctx) {
        String token = Sputnik.token(ctx);

        if (token == null || token.isBlank()) {
            return;
        }

        UUID user = authService.authenticate(token);
        if (user == null) {
            throw new APIException(401, "Invalid or expired session");
        }

        ctx.attribute(AUTHENTICATED_USER, user);
    }

    private boolean requiresAuthentication(Context ctx) {
        String method = ctx.method().name();
        String path = ctx.path();

        // Public endpoints
        if (path.equals("/api/health")
                || path.equals("/api/auth/register")
                || path.equals("/api/auth/login")) {
            return false;
        }

        // Public GET endpoints
        if (method.equals("GET")) {
            if (path.matches("/api/assets/[^/]+")
                    || path.matches("/api/user/[^/]+")
                    || path.matches("/api/user/[^/]+/avatar")
                    || path.equals("/api/chatrooms/listing")
                    || path.matches("/api/chatrooms/invite/[^/]+")
                    || path.matches("/api/chatrooms/[^/]+/overview")
                    || path.matches("/api/chatrooms/[^/]+/avatar")
                    || path.matches("/api/chatrooms/[^/]+/(history|search)")) {
                return false;
            }
        }

        return true;
    }
}