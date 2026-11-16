package np.com.milapmagar.book.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import np.com.milapmagar.book.config.OpenApiConfig;
import np.com.milapmagar.book.dto.BookDetailResponse;
import np.com.milapmagar.book.dto.BookRequest;
import np.com.milapmagar.book.dto.BookSummaryResponse;
import np.com.milapmagar.book.dto.PageResponse;
import np.com.milapmagar.book.services.BookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Tag(name = "Books", description = "Reading is public. Creating, changing and deleting needs an admin token.")
@RestController
@RequestMapping("/api/v1/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    // fetch a page of books, optionally filtered
    @Operation(summary = "List books", description = "Paged. All filters are optional and can be combined.")
    @GetMapping
    public ResponseEntity<PageResponse<BookSummaryResponse>> getBooks(
            @Parameter(description = "Matches the title or the author, ignoring case")
            @RequestParam(required = false) String search,
            @Parameter(description = "Category slug, e.g. programming")
            @RequestParam(required = false) String category,
            @Parameter(description = "Matches the author, ignoring case")
            @RequestParam(required = false) String author,
            @Parameter(description = "Page number, starting at 0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Books per page, at most 50")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "title, createdAt, publishedYear or ratingAvg, then ,asc or ,desc", example = "createdAt,desc")
            @RequestParam(required = false) String sort
    ){
        return ResponseEntity.ok(bookService.getBooks(search, category, author, page, size, sort));
    }

    // fetch by books-id
    @Operation(summary = "Get one book")
    @GetMapping("/{id}")
    public ResponseEntity<BookDetailResponse> getBookById(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.getBookById(id));
    }

    // add books
    @Operation(summary = "Create a book (admin)", security = @SecurityRequirement(name = OpenApiConfig.BEARER))
    @PostMapping
    public ResponseEntity<BookDetailResponse> createBook(@Valid @RequestBody BookRequest request) {
        BookDetailResponse book = bookService.createBook(request);
        // 201 Created plus a Location header that says where the new book can be fetched
        return ResponseEntity.created(URI.create("/api/v1/books/" + book.id())).body(book);
    }

    // replace a book's fields
    @Operation(summary = "Update a book (admin)", security = @SecurityRequirement(name = OpenApiConfig.BEARER))
    @PutMapping("/{id}")
    public ResponseEntity<BookDetailResponse> updateBook(@PathVariable Long id, @Valid @RequestBody BookRequest request) {
        return ResponseEntity.ok(bookService.updateBook(id, request));
    }

    // delete book by id
    @Operation(summary = "Delete a book and its reviews (admin)", security = @SecurityRequirement(name = OpenApiConfig.BEARER))
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }
}
