package np.com.milapmagar.book.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex){
        return respond(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // @Valid on a request body failed
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex){
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));

        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(ErrorResponse.of(status, "Validation failed", fieldErrors));
    }

    // e.g. GET /api/v1/books/abc where the id must be a number
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex){
        return respond(HttpStatus.BAD_REQUEST, "Invalid value for '" + ex.getName() + "'");
    }

    // missing or malformed JSON body
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex){
        return respond(HttpStatus.BAD_REQUEST, "Request body is missing or malformed");
    }

    // everything else
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex){
        // Spring's own web exceptions (unknown URL, wrong HTTP method, ...) already know their status.
        if(ex instanceof org.springframework.web.ErrorResponse springError){
            HttpStatus status = HttpStatus.valueOf(springError.getStatusCode().value());
            return respond(status, springError.getBody().getDetail());
        }

        // a real bug: keep the stack trace in the logs and out of the response.
        log.error("Unhandled exception", ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong");
    }

    private ResponseEntity<ErrorResponse> respond(HttpStatus status, String message){
        return ResponseEntity.status(status).body(ErrorResponse.of(status, message));
    }
}
