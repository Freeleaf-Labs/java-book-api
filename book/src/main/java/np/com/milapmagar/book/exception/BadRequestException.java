package np.com.milapmagar.book.exception;

// The request itself is wrong in a way bean validation cannot see, e.g. an unknown sort field.
// Becomes a 400.
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
