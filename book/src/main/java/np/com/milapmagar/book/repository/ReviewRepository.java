package np.com.milapmagar.book.repository;

import np.com.milapmagar.book.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    // the reviewer's name is shown next to each review, so load the user in the same query
    @EntityGraph(attributePaths = "user")
    Page<Review> findByBookId(Long bookId, Pageable pageable);

    Optional<Review> findByBookIdAndUserId(Long bookId, Long userId);

    long countByBookId(Long bookId);

    @Query("select coalesce(avg(r.rating), 0) from Review r where r.book.id = :bookId")
    double averageRating(@Param("bookId") Long bookId);

    void deleteByBookId(Long bookId);
}
