package np.com.milapmagar.book;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
public class RootController {

    // Opening the bare URL lands on the API documentation (Swagger UI)
    // instead of the Whitelabel 404 page.
    @Hidden // keep this redirect itself out of the documentation
    @GetMapping("/")
    ResponseEntity<Void> index() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/swagger-ui/index.html"))
                .build();
    }
}
