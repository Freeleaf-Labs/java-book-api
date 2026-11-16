package np.com.milapmagar.book.dto;

import np.com.milapmagar.book.model.Role;
import np.com.milapmagar.book.model.User;

import java.time.Instant;

// What the API says about a user. The password hash is deliberately not here.
public record UserResponse(
        Long id,
        String email,
        String displayName,
        Role role,
        boolean enabled,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt());
    }
}
