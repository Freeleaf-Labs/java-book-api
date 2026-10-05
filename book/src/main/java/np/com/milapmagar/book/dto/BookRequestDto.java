package np.com.milapmagar.book.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BookRequestDto(
        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must be at most 255 characters")
        String title,

        @NotBlank(message = "Author is required")
        @Size(max = 255, message = "Author must be at most 255 characters")
        String author,

        @Size(max = 255, message = "Publisher must be at most 255 characters")
        String publisher,

        @Pattern(regexp = "\\d{4}", message = "Published year must be 4 digits")
        String publishedYear,

        @NotBlank(message = "ISBN is required")
        @Pattern(regexp = "[0-9Xx-]{10,17}", message = "ISBN must be 10 to 17 digits or hyphens")
        String isbn
) {
    /* Basically it is the request format the client sends, which is w/o Id.
    * The rules above only run when the controller parameter has @Valid.
    * {
    *   "title": "how to learn java",
    *   "author": "Nick Vandik",
    *   "publisher" : "Oreily's",
    *   "publishedYear" : "2024",
    *   "isbn" : "9780132350884"
    * }
    * */
}
