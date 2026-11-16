package np.com.milapmagar.book.dto.authentication;

import np.com.milapmagar.book.dto.UserResponse;

public record AuthResponse(
        // send this back as: Authorization: Bearer <accessToken>
        String accessToken,
        // seconds until the access token stops working
        long expiresIn,
        UserResponse user
) {}
