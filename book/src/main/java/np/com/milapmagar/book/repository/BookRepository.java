package np.com.milapmagar.book.repository;

import np.com.milapmagar.book.model.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Long> {
    /*
    * Because this extends JpaRepository, the everyday methods already exist without writing them:
    *  save(), findById(), findAll(), deleteById(), count() ...
    *
    * Methods below are either "derived queries" (Spring builds the SQL from the method name)
    * or a hand-written JPQL query where the name alone would get too long.
     * */

    boolean existsByIsbn(String isbn);

    // "is this ISBN used by a book other than the one being edited?"
    boolean existsByIsbnAndIdNot(String isbn, Long id);

    boolean existsByCategoryId(Long categoryId);

    // The list endpoint. Every filter is optional: a null parameter switches its line off.
    // The two "like" parameters must arrive lower-cased and wrapped in %...%.
    // @EntityGraph loads each book's category in the same query, so a page of 20 books
    // is one query instead of 1 + 20 (the "N+1" problem).
    @EntityGraph(attributePaths = "category")
    @Query("""
            select b from Book b left join b.category c
            where (:search is null or lower(b.title) like :search or lower(b.author) like :search)
              and (:author is null or lower(b.author) like :author)
              and (:category is null or c.slug = :category)
            """)
    Page<Book> search(@Param("search") String search,
                      @Param("author") String author,
                      @Param("category") String category,
                      Pageable pageable);
}
