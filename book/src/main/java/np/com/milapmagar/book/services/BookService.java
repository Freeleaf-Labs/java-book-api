package np.com.milapmagar.book.services;

import np.com.milapmagar.book.dto.BookDetailResponse;
import np.com.milapmagar.book.dto.BookRequest;
import np.com.milapmagar.book.dto.BookSummaryResponse;
import np.com.milapmagar.book.dto.PageResponse;
import np.com.milapmagar.book.exception.BadRequestException;
import np.com.milapmagar.book.exception.ConflictException;
import np.com.milapmagar.book.exception.ResourceNotFoundException;
import np.com.milapmagar.book.model.Book;
import np.com.milapmagar.book.model.Category;
import np.com.milapmagar.book.repository.BookRepository;
import np.com.milapmagar.book.repository.CategoryRepository;
import np.com.milapmagar.book.repository.ReviewRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@Transactional(readOnly = true)
public class BookService {

    private static final Set<String> SORT_FIELDS = Set.of("title", "createdAt", "publishedYear", "ratingAvg");

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final ReviewRepository reviewRepository;

    public BookService(BookRepository bookRepository,
                       CategoryRepository categoryRepository,
                       ReviewRepository reviewRepository) {
        this.bookRepository = bookRepository;
        this.categoryRepository = categoryRepository;
        this.reviewRepository = reviewRepository;
    }

    public PageResponse<BookSummaryResponse> getBooks(String search, String category, String author,
                                                      int page, int size, String sort) {
        Pageable pageable = Paging.of(page, size, sort, "createdAt,desc", SORT_FIELDS);
        Page<Book> books = bookRepository.search(likePattern(search), likePattern(author), blankToNull(category), pageable);
        return PageResponse.from(books.map(BookSummaryResponse::from));
    }

    public BookDetailResponse getBookById(Long id){
        return BookDetailResponse.from(findBook(id));
    }

    @Transactional
    public BookDetailResponse createBook(BookRequest request) {
        String isbn = blankToNull(request.isbn());
        if (isbn != null && bookRepository.existsByIsbn(isbn)) {
            throw new ConflictException("A book with ISBN " + isbn + " already exists");
        }

        Book book = new Book();
        copyFields(request, isbn, book);
        return BookDetailResponse.from(bookRepository.save(book));
    }

    @Transactional
    public BookDetailResponse updateBook(Long id, BookRequest request) {
        Book book = findBook(id);

        String isbn = blankToNull(request.isbn());
        if (isbn != null && bookRepository.existsByIsbnAndIdNot(isbn, id)) {
            throw new ConflictException("A book with ISBN " + isbn + " already exists");
        }

        copyFields(request, isbn, book);
        // Flushing now (instead of at commit) runs the UPDATE before the response is built,
        // so the response carries the new updatedAt.
        return BookDetailResponse.from(bookRepository.saveAndFlush(book));
    }

    @Transactional
    public void deleteBook(Long id) {
        Book book = findBook(id);
        // the reviews point at the book, so they have to go first
        reviewRepository.deleteByBookId(id);
        bookRepository.delete(book);
    }

    private Book findBook(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book with id " + id + " not found"));
    }

    private void copyFields(BookRequest request, String isbn, Book book) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new BadRequestException("Category with id " + request.categoryId() + " does not exist"));

        book.setTitle(request.title().trim());
        book.setAuthor(request.author().trim());
        book.setPublisher(blankToNull(request.publisher()));
        book.setPublishedYear(request.publishedYear());
        book.setIsbn(isbn);
        book.setDescription(blankToNull(request.description()));
        book.setCategory(category);
    }

    // "  " and "" mean "not given"; storing them as null keeps one meaning for "empty"
    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    // "Java" -> "%java%", ready for a case-insensitive LIKE; null when there is nothing to search for
    private static String likePattern(String value) {
        String text = blankToNull(value);
        return text == null ? null : "%" + text.toLowerCase() + "%";
    }
}
