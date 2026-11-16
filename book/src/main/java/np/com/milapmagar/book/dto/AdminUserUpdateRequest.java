package np.com.milapmagar.book.dto;

import np.com.milapmagar.book.model.Role;

// Both fields are optional: only the ones that are sent get changed.
public record AdminUserUpdateRequest(
        Role role,
        Boolean enabled
) {}
