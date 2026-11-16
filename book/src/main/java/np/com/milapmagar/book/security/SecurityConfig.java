package np.com.milapmagar.book.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    // Security Password encoder
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** JWT: one secret key signs the tokens (encoder) and checks them (decoder). **/

    @Bean
    public SecretKey jwtSecretKey(@Value("${app.jwt.secret}") String secret) {
        byte[] keyBytes;
        if (secret.isBlank()) {
            // Safe default: nobody can know this key. The cost is that access tokens stop working
            // when the app restarts (the browser quietly gets a new one through /auth/refresh).
            keyBytes = new byte[32];
            new SecureRandom().nextBytes(keyBytes);
            log.warn("JWT_SECRET is not set: using a random key that changes on every restart");
        } else {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            if (keyBytes.length < 32) {
                throw new IllegalStateException("JWT_SECRET must be at least 32 characters long");
            }
        }
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
        return NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
    }

    /** The rules: who may call what. The first matching line wins, so order matters. **/

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver
    ) throws Exception {

        // 401 and 403 are decided in the security filters, before any controller runs, so the
        // @RestControllerAdvice never sees them on its own. Handing the exception to Spring MVC's
        // resolver sends it through GlobalExceptionHandler, and the client gets the usual JSON.
        AuthenticationEntryPoint unauthorized = (request, response, ex) ->
                exceptionResolver.resolveException(request, response, null, ex);
        AccessDeniedHandler forbidden = (request, response, ex) ->
                exceptionResolver.resolveException(request, response, null, ex);

        // The token carries a "role" claim (USER / ADMIN); Spring Security wants ROLE_USER / ROLE_ADMIN.
        JwtAuthenticationConverter roleFromToken = new JwtAuthenticationConverter();
        roleFromToken.setJwtGrantedAuthoritiesConverter(jwt ->
                List.of(new SimpleGrantedAuthority("ROLE_" + jwt.getClaimAsString("role"))));

        // Login, refresh and logout must work even when the browser still sends an expired
        // access token, so the Authorization header is ignored on the auth endpoints.
        DefaultBearerTokenResolver headerToken = new DefaultBearerTokenResolver();

        http
                .csrf(AbstractHttpConfigurer::disable)   // no session cookie is used for auth, so nothing to forge
                .cors(Customizer.withDefaults())          // uses the CorsConfigurationSource bean in CorsConfig
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // a user's own review: any logged-in user (must come before the two book rules)
                        .requestMatchers("/api/v1/books/*/reviews/mine").authenticated()

                        // reading the catalogue needs no account.
                        // (/resources is not built yet; left public so it answers 404 "not there", not 401)
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/books/**", "/api/v1/categories", "/api/v1/resources/**").permitAll()

                        // changing the catalogue, and everything under /admin: admins only
                        .requestMatchers("/api/v1/admin/**", "/api/v1/books/**", "/api/v1/categories/**").hasRole("ADMIN")

                        // register / login / refresh / logout, the docs and the health check
                        .requestMatchers("/api/v1/auth/**", "/", "/error",
                                "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**",
                                "/actuator/health/**").permitAll()

                        // everything else: any logged-in user
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .bearerTokenResolver(request ->
                                request.getRequestURI().startsWith("/api/v1/auth/") ? null : headerToken.resolve(request))
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(roleFromToken))
                        .authenticationEntryPoint(unauthorized)
                        .accessDeniedHandler(forbidden))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(unauthorized)
                        .accessDeniedHandler(forbidden));
        return http.build();
    }
}
