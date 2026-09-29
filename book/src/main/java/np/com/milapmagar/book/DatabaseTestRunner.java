package np.com.milapmagar.book;

import np.com.milapmagar.book.model.Book;
import np.com.milapmagar.book.repository.BookRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DatabaseTestRunner implements CommandLineRunner {

    // You are asking Spring to inject (hand you) the BookRepository Bean here!
    private final BookRepository bookRepository;

    public DatabaseTestRunner(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println(">>> TESTING REPOSITORY BEAN...");

        // 1. Create a fake book (Make sure your Book model has a title field)
        Book book = new Book();
        book.setTitle("Mastering Spring Boot");
        String TEST_ISBN = "9780000000001";
        book.setIsbn(TEST_ISBN);

        // 2. Save it using the built-in method (Zero method bodies required!)
        if (bookRepository.findByIsbn(TEST_ISBN).isEmpty()) {
            bookRepository.save(book);
        }

        // 3. Count how many books are in the database
        long totalBooks = bookRepository.count();
        System.out.println(">>> SUCCESS! Total books in database: " + totalBooks);
    }
}