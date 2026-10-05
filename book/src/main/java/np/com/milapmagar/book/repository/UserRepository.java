package np.com.milapmagar.book.repository;

import np.com.milapmagar.book.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // normally empty but can create some queries for trying out.
    Optional<User> findByEmail(String email);

    String existsByEmail(String email);
}
