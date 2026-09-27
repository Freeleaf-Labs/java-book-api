package np.com.milapmagar.book.exception;

import java.time.LocalTime;

public record ErrorResponse(
        LocalTime timeStamp,
        int status,
        String error,
        String message
) { }
