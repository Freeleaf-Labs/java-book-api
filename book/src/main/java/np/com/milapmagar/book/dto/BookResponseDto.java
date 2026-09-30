package np.com.milapmagar.book.dto;

public record BookResponseDto(
        Long id,
        String title,
        String author,
        String publisher,
        String publishedYear,
        String isbn,
        Double price
) {}
