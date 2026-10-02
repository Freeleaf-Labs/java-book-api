package np.com.milapmagar.book.controller;

import np.com.milapmagar.book.dto.BookResponseDto;
import np.com.milapmagar.book.services.BookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    // fetch for all books
    @GetMapping
    public ResponseEntity<List<BookResponseDto>> getBooks(){
        List<BookResponseDto> books = bookService.getBooks();
        return ResponseEntity.ok(books);
    }
    // fetch by books-id
    @GetMapping("/{id}")
    public ResponseEntity<BookResponseDto> getBookById(@PathVariable Long id) {
        BookResponseDto bookDto = bookService.getBookById(id);
        return ResponseEntity.ok(bookDto);
    }

    // add books

    // update books by title, author, publisher

    // delete book by id
}
