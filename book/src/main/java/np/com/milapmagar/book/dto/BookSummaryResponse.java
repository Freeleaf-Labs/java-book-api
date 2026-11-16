package np.com.milapmagar.book.dto;

import np.com.milapmagar.book.model.Book;

// The short form of a book, used in lists.
public record BookSummaryResponse(
        Long id,
        String title,
        String author,
        Integer publishedYear,
        CategoryResponse category,
        String coverUrl,
        double ratingAvg,
        int ratingCount,
        // always false for now: there is no file storage yet
        boolean hasFile
) {
    public static BookSummaryResponse from(Book book) {
        return new BookSummaryResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getPublishedYear(),
                book.getCategory() == null ? null : CategoryResponse.from(book.getCategory()),
                book.getCoverUrl(),
                book.getRatingAvg(),
                book.getRatingCount(),
                false);
    }
}
