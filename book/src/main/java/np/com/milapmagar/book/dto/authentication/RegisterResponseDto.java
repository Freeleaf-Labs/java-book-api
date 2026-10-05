package np.com.milapmagar.book.dto.authentication;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterResponseDto(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "FullName is required")
        @Size(max = 255, message = "Full Name must be with in 255 characters")
        String fullName,


        @NotBlank(message = "Phone is required")
        @Size(max = 10,  message = "Phone must be with in 10 characters")
        String phone
) {}
