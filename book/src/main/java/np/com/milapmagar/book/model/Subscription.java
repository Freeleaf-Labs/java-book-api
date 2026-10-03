package np.com.milapmagar.book.model;

import jakarta.persistence.*;
import java.time.LocalDate;

public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String plan;

    private LocalDate startDate;
    private LocalDate endDate;

    private boolean active;

    @OneToOne
    private User user;
}
