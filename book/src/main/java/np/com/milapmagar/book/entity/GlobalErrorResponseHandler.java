package np.com.milapmagar.book.entity;

import np.com.milapmagar.book.exception.ErrorResponse;
import np.com.milapmagar.book.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.time.LocalTime;

public class GlobalErrorResponseHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex){
        ErrorResponse er = new ErrorResponse(
                LocalTime.now(),
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage()
        );
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
}
