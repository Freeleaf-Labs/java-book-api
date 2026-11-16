package np.com.milapmagar.book.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import np.com.milapmagar.book.config.OpenApiConfig;
import np.com.milapmagar.book.dto.AdminStatsResponse;
import np.com.milapmagar.book.dto.AdminUserUpdateRequest;
import np.com.milapmagar.book.dto.PageResponse;
import np.com.milapmagar.book.dto.UserResponse;
import np.com.milapmagar.book.security.CurrentUser;
import np.com.milapmagar.book.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Everything under /api/v1/admin is limited to admins by one rule in SecurityConfig,
// so the methods here need no role checks of their own.
@Tag(name = "Admin", description = "Needs an admin token.")
@SecurityRequirement(name = OpenApiConfig.BEARER)
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "List users, newest first")
    @GetMapping("/users")
    public ResponseEntity<PageResponse<UserResponse>> getUsers(
            @Parameter(description = "Matches the email or the display name, ignoring case")
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(userService.getUsers(search, page, size));
    }

    @Operation(summary = "Change a user's role, or enable / disable the account",
            description = "Send only the fields to change. You cannot change your own account (409).")
    @PatchMapping("/users/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id,
                                                   @RequestBody AdminUserUpdateRequest request,
                                                   Authentication authentication) {
        return ResponseEntity.ok(userService.updateUser(id, request, CurrentUser.id(authentication)));
    }

    @Operation(summary = "Totals for the admin dashboard")
    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> getStats() {
        return ResponseEntity.ok(userService.getStats());
    }
}
