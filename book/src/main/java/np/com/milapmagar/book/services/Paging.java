package np.com.milapmagar.book.services;

import np.com.milapmagar.book.exception.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

// Turns the ?page=&size=&sort= query parameters into a Pageable, in one place for every list.
public final class Paging {

    public static final int MAX_SIZE = 50;

    private Paging() {
    }

    /**
     * @param sort          "field,asc" or "field,desc"; null or blank falls back to defaultSort
     * @param allowedFields the only fields a client may sort by. Without this list a client could
     *                      sort by any entity field, including ones that are slow or private.
     */
    public static Pageable of(int page, int size, String sort, String defaultSort, Set<String> allowedFields) {
        if (page < 0) throw new BadRequestException("page must be 0 or more");
        if (size < 1) throw new BadRequestException("size must be 1 or more");

        String[] parts = (sort == null || sort.isBlank() ? defaultSort : sort).split(",");
        String field = parts[0].trim();
        String direction = parts.length > 1 ? parts[1].trim().toLowerCase() : "asc";

        if (!allowedFields.contains(field)) {
            throw new BadRequestException("Cannot sort by '" + field + "'");
        }
        if (!direction.equals("asc") && !direction.equals("desc")) {
            throw new BadRequestException("Sort direction must be 'asc' or 'desc'");
        }

        // The id is a tie-breaker: rows with equal values keep one fixed order,
        // so the same row can never show up on two different pages.
        Sort order = Sort.by(Sort.Direction.fromString(direction), field).and(Sort.by("id"));
        return PageRequest.of(page, Math.min(size, MAX_SIZE), order);
    }

    // plain newest-first paging for lists that have no sort parameter
    public static Pageable newestFirst(int page, int size) {
        return of(page, size, null, "createdAt,desc", Set.of("createdAt"));
    }
}
