package np.com.milapmagar.book.dto;

import np.com.milapmagar.book.model.Book;

public record BookResponseDto(
        Long id,
        String title,
        String author,
        String publisher,
        String publishedYear,
        String isbn
) {
    // the one place an entity is turned into a response
    public static BookResponseDto from(Book book){
        return new BookResponseDto(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getPublisher(),
                book.getPublishedYear(),
                book.getIsbn());
    }
}
