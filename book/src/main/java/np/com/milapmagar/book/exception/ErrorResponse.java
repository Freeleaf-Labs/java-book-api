package np.com.milapmagar.book.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Map;

// The one JSON shape every error leaves the API in.
public record ErrorResponse(
        // time with no date is useless so we use Instant
        Instant timestamp,
        int status,
        String error,
        String message,
        // only present on validation failures, e.g. {"title": "must not be blank"}
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Map<String, String> fieldErrors
) {
    public static ErrorResponse of(HttpStatus status, String message){
        return new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message, null);
    }

    public static ErrorResponse of(HttpStatus status, String message, Map<String, String> fieldErrors){
        return new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message, fieldErrors);
    }
}
