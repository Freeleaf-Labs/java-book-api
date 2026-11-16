package np.com.milapmagar.book.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import np.com.milapmagar.book.dto.UserResponse;
import np.com.milapmagar.book.dto.authentication.AuthResponse;
import np.com.milapmagar.book.dto.authentication.LoginRequest;
import np.com.milapmagar.book.dto.authentication.RegisterRequest;
import np.com.milapmagar.book.services.AuthServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@Tag(name = "Auth", description = "Register, log in, and keep a session alive. No token needed.")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String REFRESH_COOKIE = "refresh_token";

    private final AuthServices authServices;
    private final String cookieSameSite;

    public AuthController(AuthServices authServices,
                          @Value("${app.cookie.same-site}") String cookieSameSite) {
        this.authServices = authServices;
        this.cookieSameSite = cookieSameSite;
    }

    @Operation(summary = "Create an account", description = "Returns the new user. Log in afterwards to get a token.")
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authServices.register(request));
    }

    @Operation(summary = "Log in", description = "Returns an access token (15 minutes) and sets the refresh-token cookie (7 days).")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return withRefreshCookie(authServices.login(request), http);
    }

    @Operation(summary = "Get a new access token", description = "Uses the refresh-token cookie; no body. The cookie is replaced with a new one.")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @Parameter(hidden = true) @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
            HttpServletRequest http
    ) {
        return withRefreshCookie(authServices.refresh(refreshToken), http);
    }

    @Operation(summary = "Log out", description = "Revokes the refresh token and clears the cookie.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Parameter(hidden = true) @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
            HttpServletRequest http
    ) {
        authServices.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookie("", Duration.ZERO, http).toString()) // max-age 0 deletes it
                .build();
    }

    private ResponseEntity<AuthResponse> withRefreshCookie(AuthServices.Session session, HttpServletRequest http) {
        ResponseCookie cookie = refreshCookie(session.refreshToken(), AuthServices.REFRESH_TOKEN_LIFETIME, http);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(session.response());
    }

    private ResponseCookie refreshCookie(String value, Duration maxAge, HttpServletRequest http) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)              // JavaScript cannot read it, so a script injected into the page cannot steal it
                .secure(http.isSecure())     // https only in production; plain http still works on localhost
                .sameSite(cookieSameSite)
                .path("/api/v1/auth")        // the browser sends it to the auth endpoints and nowhere else
                .maxAge(maxAge)
                .build();
    }
}
