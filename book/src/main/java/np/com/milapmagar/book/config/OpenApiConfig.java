package np.com.milapmagar.book.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

// Describes the API for Swagger UI (served at "/").
// The security scheme below is what adds the "Authorize" button; endpoints that need a login are
// marked with @SecurityRequirement(name = "bearerAuth") in the controllers and show a padlock.
@Configuration
@OpenAPIDefinition(info = @Info(
        title = "Book API",
        version = "v1",
        description = """
                Books, categories, reviews and accounts.

                **Reading the catalogue is public. Everything else needs a login:**

                1. Create an account with `POST /api/v1/auth/register` (skip this if you have one).
                2. Call `POST /api/v1/auth/login` and copy the `accessToken` from the response.
                3. Click **Authorize** at the top right, paste the token and confirm.

                The token lasts 15 minutes; `POST /api/v1/auth/refresh` gives you a new one.
                Endpoints tagged *Admin*, and writes to books and categories, need an admin account.
                """))
@SecurityScheme(
        name = OpenApiConfig.BEARER,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
public class OpenApiConfig {

    public static final String BEARER = "bearerAuth";
}
