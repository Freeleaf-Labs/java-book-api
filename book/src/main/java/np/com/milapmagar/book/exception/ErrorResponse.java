package np.com.milapmagar.book.exception;

import java.time.Instant;

public class ErrorResponse {
    // time with no date is useless so we use Instant
    Instant timeStamp;
    int status;
    String error;
    String message;

    public ErrorResponse(int status, String error, String message){
        this.timeStamp = Instant.now();
        this.status = status;
        this.error = error;
        this.message = message;
    }

    // getters and setter here.
}
