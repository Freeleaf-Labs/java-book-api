package np.com.milapmagar.book.repository;

import np.com.milapmagar.book.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {
    // Leave this section empty as it creates automatically by spring boot
    /*
    * Moreover as we extended the Book model we do not need to create methods like
    *  GET - save()
    *  GET BY ID - findById(int id)
    *  POST - add()
    *  PUT / PATCH - updateById(int id)
    *  DELETE BY ID - delete(int id)
    *
    * So, those above methods are created automatically without any callouts and other
    * effort that is basically the beauty of spring boot API's.
     * */

    // if only is custom query needed then you can add like findByTitle(String Title)
    // -> which can basically be for finding the title and all sort of jobs

    // Custom Queries tryouts
    // author names
    List<Book> findByAuthor(String author);
    // ISBN number
    Optional<Book> findByIsbn(String isbn);
    boolean existsByIsbn(String isbn);
    // titles
    List<Book> findByTitle(String title);
    List<Book> findByTitleContainingIgnoreCase(String title);
}
