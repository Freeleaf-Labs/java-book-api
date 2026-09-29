package np.com.milapmagar.book;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {

    private final String appName;

    RootController(@Value("${spring.application.name}") String appName) {
        this.appName = appName;
    }

    // Landing response for "/" so the deployed URL shows what this service is
    // instead of the Whitelabel 404 page.
    @GetMapping("/")
    ApiInfo index() {
        return new ApiInfo(
                appName,
                "running",
                Map.of(
                        "health", "/actuator/health"
                        // add "books": "/api/books" once BookController has routes
                ));
    }

    record ApiInfo(String name, String status, Map<String, String> links) {
    }
}
