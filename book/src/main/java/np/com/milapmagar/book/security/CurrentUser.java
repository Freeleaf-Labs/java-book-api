package np.com.milapmagar.book.security;

import org.springframework.security.core.Authentication;

// Reads the caller's identity from the verified token. Controllers use this instead of trusting
// a user id from the URL or the request body, which a client could simply change.
public final class CurrentUser {

    private CurrentUser() {
    }

    // the token's subject is the user id (see JwtService)
    public static Long id(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }

    public static boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
