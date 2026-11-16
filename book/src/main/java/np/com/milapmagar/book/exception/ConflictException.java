package np.com.milapmagar.book.exception;

// The request is fine on its own but clashes with what is already stored: a duplicate email,
// a duplicate ISBN, a category that is still in use. Becomes a 409.
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
