package np.com.milapmagar.book.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BookRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must be at most 255 characters")
        String title,

        @NotBlank(message = "Author is required")
        @Size(max = 255, message = "Author must be at most 255 characters")
        String author,

        @Size(max = 255, message = "Publisher must be at most 255 characters")
        String publisher,

        @Min(value = 0, message = "Published year must be a valid year")
        @Max(value = 2100, message = "Published year must be a valid year")
        Integer publishedYear,

        // optional: empty is allowed, anything else must look like an ISBN
        @Pattern(regexp = "^$|[0-9Xx-]{10,17}", message = "ISBN must be 10 to 17 digits or hyphens")
        String isbn,

        @Size(max = 5000, message = "Description must be at most 5000 characters")
        String description,

        @NotNull(message = "Category is required")
        Long categoryId
) {
    /* What the client sends to create or replace a book. There is no id: the server owns it.
    * The rules above only run when the controller parameter has @Valid.
    * {
    *   "title": "Clean Code",
    *   "author": "Robert C. Martin",
    *   "publisher": "Prentice Hall",
    *   "publishedYear": 2008,
    *   "isbn": "9780132350884",
    *   "description": "...",
    *   "categoryId": 1
    * }
    * */
}
