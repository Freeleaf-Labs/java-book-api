package np.com.milapmagar.book.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import np.com.milapmagar.book.model.Book;
import np.com.milapmagar.book.model.User;

public record LoginRequestDto(

        @NotBlank(message = "Email is required")
        @Email(message = "Title must be at most 255 characters")
        String title,

        @NotBlank(message = "Password is required")
        @Size(min= 8, max = 255, message = "Password must be atleast 8 - 16 ")
        String password
) {}
