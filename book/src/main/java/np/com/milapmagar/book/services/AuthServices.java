package np.com.milapmagar.book.services;

import np.com.milapmagar.book.dto.UserResponse;
import np.com.milapmagar.book.dto.authentication.AuthResponse;
import np.com.milapmagar.book.dto.authentication.LoginRequest;
import np.com.milapmagar.book.dto.authentication.RegisterRequest;
import np.com.milapmagar.book.exception.ConflictException;
import np.com.milapmagar.book.exception.InvalidCredentialsException;
import np.com.milapmagar.book.model.RefreshToken;
import np.com.milapmagar.book.model.Role;
import np.com.milapmagar.book.model.User;
import np.com.milapmagar.book.repository.RefreshTokenRepository;
import np.com.milapmagar.book.repository.UserRepository;
import np.com.milapmagar.book.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class AuthServices {

    public static final Duration REFRESH_TOKEN_LIFETIME = Duration.ofDays(7);

    // One message for "no such email", "wrong password" and "disabled", so a stranger
    // cannot use the login form to find out which emails have an account.
    private static final String BAD_LOGIN = "Invalid email or password";
    private static final String BAD_SESSION = "Session expired, please log in again";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SecureRandom random = new SecureRandom();

    public AuthServices(UserRepository userRepository,
                        RefreshTokenRepository refreshTokenRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /** What a successful login or refresh produces: the JSON body, plus the raw refresh token
     *  that the controller puts in a cookie. The raw token exists only here and in the browser. */
    public record Session(AuthResponse response, String refreshToken) {}

    // register-api-services
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email is already registered");
        }

        // Hash first: the raw password never reaches the database.
        String passwordHash = passwordEncoder.encode(request.password());
        User user = new User(email, passwordHash, request.displayName().trim(), Role.USER);
        return UserResponse.from(userRepository.save(user));
    }

    // login-api-services
    @Transactional
    public Session login(LoginRequest request) {
        User user = userRepository.findByEmail(normalize(request.email()))
                .orElseThrow(() -> new InvalidCredentialsException(BAD_LOGIN));

        if (!user.isEnabled() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException(BAD_LOGIN);
        }
        return openSession(user);
    }

    // Swaps a refresh token for a new access token. The old refresh token is revoked and a new one
    // issued ("rotation"), so a stolen token stops working the moment the real user refreshes.
    @Transactional
    public Session refresh(String rawRefreshToken) {
        RefreshToken token = findUsable(rawRefreshToken)
                .orElseThrow(() -> new InvalidCredentialsException(BAD_SESSION));

        User user = token.getUser();
        if (!user.isEnabled()) {
            throw new InvalidCredentialsException(BAD_SESSION);
        }

        token.revoke();
        return openSession(user);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        findUsable(rawRefreshToken).ifPresent(RefreshToken::revoke);
    }

    private Session openSession(User user) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String rawRefreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        refreshTokenRepository.save(
                new RefreshToken(user, sha256(rawRefreshToken), Instant.now().plus(REFRESH_TOKEN_LIFETIME)));

        AuthResponse response = new AuthResponse(
                jwtService.issue(user),
                JwtService.ACCESS_TOKEN_LIFETIME.toSeconds(),
                UserResponse.from(user));
        return new Session(response, rawRefreshToken);
    }

    private Optional<RefreshToken> findUsable(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return Optional.empty();
        }
        return refreshTokenRepository.findByTokenHash(sha256(rawRefreshToken))
                .filter(RefreshToken::isUsable);
    }

    // "Asha@Example.com " and "asha@example.com" are the same account
    private static String normalize(String email) {
        return email.trim().toLowerCase();
    }

    private static String sha256(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e); // every JVM ships SHA-256
        }
    }
}
