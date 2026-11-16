package np.com.milapmagar.book.dto;

import np.com.milapmagar.book.model.Book;

import java.time.Instant;

// Everything about one book: the summary fields plus the long ones.
public record BookDetailResponse(
        Long id,
        String title,
        String author,
        Integer publishedYear,
        CategoryResponse category,
        String coverUrl,
        double ratingAvg,
        int ratingCount,
        boolean hasFile,
        String publisher,
        String isbn,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
    // the one place an entity is turned into a detail response
    public static BookDetailResponse from(Book book) {
        return new BookDetailResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getPublishedYear(),
                book.getCategory() == null ? null : CategoryResponse.from(book.getCategory()),
                book.getCoverUrl(),
                book.getRatingAvg(),
                book.getRatingCount(),
                false,
                book.getPublisher(),
                book.getIsbn(),
                book.getDescription(),
                book.getCreatedAt(),
                book.getUpdatedAt());
    }
}
