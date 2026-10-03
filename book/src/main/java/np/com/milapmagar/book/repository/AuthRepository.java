package np.com.milapmagar.book.repository;

import np.com.milapmagar.book.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthRepository extends JpaRepository<User, Long> { }

