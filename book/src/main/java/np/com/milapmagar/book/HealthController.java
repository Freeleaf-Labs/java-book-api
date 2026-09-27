package np.com.milapmagar.book;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    @GetMapping("/health")
    String healthCheck (){
        return "Health check!!!!! 123 nei ho ta haina ra? ho hola kina chaldina yo kina hora";
    }
}