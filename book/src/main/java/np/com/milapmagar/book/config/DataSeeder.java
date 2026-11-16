package np.com.milapmagar.book.config;

import np.com.milapmagar.book.model.Book;
import np.com.milapmagar.book.model.Category;
import np.com.milapmagar.book.model.Role;
import np.com.milapmagar.book.model.User;
import np.com.milapmagar.book.repository.BookRepository;
import np.com.milapmagar.book.repository.CategoryRepository;
import np.com.milapmagar.book.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Runs once, right after the app has started.
//  1. Makes sure there is an admin account (nobody can register as admin through the API).
//  2. Puts a starter catalogue into a completely empty database.
// Both steps do nothing when their data is already there, so restarts are harmless.
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final BookRepository bookRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;
    private final boolean seedDemoData;

    public DataSeeder(UserRepository userRepository,
                      CategoryRepository categoryRepository,
                      BookRepository bookRepository,
                      PasswordEncoder passwordEncoder,
                      @Value("${app.admin.email}") String adminEmail,
                      @Value("${app.admin.password}") String adminPassword,
                      @Value("${app.seed-demo-data}") boolean seedDemoData) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.bookRepository = bookRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail.trim().toLowerCase();
        this.adminPassword = adminPassword;
        this.seedDemoData = seedDemoData;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedAdmin();
        if (seedDemoData) {
            seedCatalogue();
        }
    }

    private void seedAdmin() {
        if (userRepository.existsByRole(Role.ADMIN)) {
            return;
        }
        if (adminEmail.isEmpty() || adminPassword.length() < 8) {
            log.warn("There is no admin account. Set ADMIN_EMAIL and ADMIN_PASSWORD (8+ characters) and restart to create one.");
            return;
        }

        // If that email already registered as a normal user, promote the account and keep its password.
        User admin = userRepository.findByEmail(adminEmail)
                .orElseGet(() -> new User(adminEmail, passwordEncoder.encode(adminPassword), "Admin", Role.USER));
        admin.setRole(Role.ADMIN);
        admin.setEnabled(true);
        userRepository.save(admin);
        log.info("Admin account ready: {}", adminEmail);
    }

    private void seedCatalogue() {
        if (categoryRepository.count() > 0 || bookRepository.count() > 0) {
            return;
        }

        Map<String, Category> categories = new LinkedHashMap<>();
        for (String name : List.of("Programming", "Computer Science", "Mathematics", "Science", "Business", "Literature")) {
            String slug = name.toLowerCase().replace(' ', '-');
            categories.put(slug, categoryRepository.save(new Category(name, slug)));
        }

        for (SeedBook seed : SEED_BOOKS) {
            Book book = new Book();
            book.setTitle(seed.title());
            book.setAuthor(seed.author());
            book.setPublishedYear(seed.year());
            book.setIsbn(seed.isbn());
            book.setDescription(seed.description());
            book.setCoverUrl("https://covers.openlibrary.org/b/isbn/" + seed.isbn() + "-L.jpg");
            book.setCategory(categories.get(seed.category()));
            bookRepository.save(book);
        }
        log.info("Seeded {} categories and {} books", categories.size(), SEED_BOOKS.size());
    }

    private record SeedBook(String isbn, String title, String author, int year, String category, String description) {}

    private static final List<SeedBook> SEED_BOOKS = List.of(
            new SeedBook("9780132350884", "Clean Code", "Robert C. Martin", 2008, "programming",
                    "A guide to writing code that other people can read and change: naming, small functions, error handling and tests."),
            new SeedBook("9780201616224", "The Pragmatic Programmer", "Andy Hunt, Dave Thomas", 1999, "programming",
                    "Practical habits for working programmers, from avoiding duplication to automating what you repeat."),
            new SeedBook("9780134685991", "Effective Java", "Joshua Bloch", 2001, "programming",
                    "Concrete advice on using Java well, organised as short items on objects, generics, lambdas and concurrency."),
            new SeedBook("9780596009205", "Head First Java", "Kathy Sierra", 2003, "programming",
                    "A visual, exercise-heavy introduction to Java and object-oriented programming for beginners."),
            new SeedBook("9780201633610", "Design Patterns", "Erich Gamma, John Vlissides", 1995, "programming",
                    "The catalogue of classic object-oriented design patterns, each with its intent, structure and trade-offs."),
            new SeedBook("9780201485677", "Refactoring", "Martin Fowler", 1999, "programming",
                    "How to improve the design of existing code in small, safe steps without changing what it does."),
            new SeedBook("9781593279509", "Eloquent JavaScript", "Marijn Haverbeke", 2009, "programming",
                    "An introduction to programming through JavaScript, covering the language, the browser and Node."),
            new SeedBook("9780131103627", "The C Programming Language", "Brian W. Kernighan, Dennis M. Ritchie", 1978, "programming",
                    "The concise reference and tutorial for the C language, written by its creators."),
            new SeedBook("9780262510875", "Structure and Interpretation of Computer Programs", "Harold Abelson, Gerald Jay Sussman", 1985, "computer-science",
                    "A foundational text on how programs are built from abstractions, taught through Scheme."),
            new SeedBook("9780262033848", "Introduction to Algorithms", "Thomas H. Cormen, Charles E. Leiserson", 1990, "computer-science",
                    "A comprehensive textbook on algorithms and data structures with proofs and pseudocode."),
            new SeedBook("9781118063330", "Operating System Concepts", "Abraham Silberschatz", 1988, "computer-science",
                    "How operating systems work: processes, memory, storage, scheduling and protection."),
            new SeedBook("9781449373320", "Designing Data-Intensive Applications", "Martin Kleppmann", 2017, "computer-science",
                    "How databases and distributed systems store, replicate and process data, and the trade-offs involved."),
            new SeedBook("9781285740621", "Calculus", "James Stewart", 1986, "mathematics",
                    "A widely used calculus textbook covering limits, derivatives, integrals and series with many exercises."),
            new SeedBook("9783319110790", "Linear Algebra Done Right", "Sheldon Axler", 2015, "mathematics",
                    "A proof-based second course in linear algebra that builds the subject around linear maps."),
            new SeedBook("9780691164076", "How to Solve It", "George Pólya", 1945, "mathematics",
                    "A short classic on problem-solving method: understand, plan, carry out, look back."),
            new SeedBook("9780553380163", "A Brief History of Time", "Stephen Hawking", 1988, "science",
                    "A popular account of cosmology, black holes and the nature of time for general readers."),
            new SeedBook("9780345539434", "Cosmos", "Carl Sagan", 1980, "science",
                    "A tour of the universe and the history of science, written for a general audience."),
            new SeedBook("9780374533557", "Thinking, Fast and Slow", "Daniel Kahneman", 2011, "business",
                    "How two modes of thinking shape judgement and decisions, drawn from decades of psychology research."),
            new SeedBook("9780307887894", "The Lean Startup", "Eric Ries", 2011, "business",
                    "An approach to building products by testing assumptions early with small experiments."),
            new SeedBook("9780451524935", "Nineteen Eighty-Four", "George Orwell", 1949, "literature",
                    "A dystopian novel about surveillance, propaganda and the control of truth."),
            new SeedBook("9780061120084", "To Kill a Mockingbird", "Harper Lee", 1960, "literature",
                    "A novel of childhood and injustice in the American South, told through a young girl's eyes."),
            new SeedBook("9780141439518", "Pride and Prejudice", "Jane Austen", 1813, "literature",
                    "A novel of manners about marriage, family and first impressions in Regency England.")
    );
}
