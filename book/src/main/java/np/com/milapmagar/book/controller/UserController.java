package np.com.milapmagar.book.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import np.com.milapmagar.book.config.OpenApiConfig;
import np.com.milapmagar.book.dto.UpdateProfileRequest;
import np.com.milapmagar.book.dto.UserResponse;
import np.com.milapmagar.book.security.CurrentUser;
import np.com.milapmagar.book.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Users", description = "The logged-in user's own account.")
@SecurityRequirement(name = OpenApiConfig.BEARER)
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // "me" is whoever the token belongs to. There is deliberately no /users/{id} here:
    // the id always comes from the token, so nobody can read or change someone else's account.
    @Operation(summary = "Get my account")
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(Authentication authentication) {
        return ResponseEntity.ok(userService.getUser(CurrentUser.id(authentication)));
    }

    @Operation(summary = "Change my display name")
    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateMe(@Valid @RequestBody UpdateProfileRequest request,
                                                 Authentication authentication) {
        return ResponseEntity.ok(userService.updateProfile(CurrentUser.id(authentication), request));
    }
}
