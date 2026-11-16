package np.com.milapmagar.book.services;

import np.com.milapmagar.book.dto.PageResponse;
import np.com.milapmagar.book.dto.ReviewRequest;
import np.com.milapmagar.book.dto.ReviewResponse;
import np.com.milapmagar.book.exception.ResourceNotFoundException;
import np.com.milapmagar.book.model.Book;
import np.com.milapmagar.book.model.Review;
import np.com.milapmagar.book.model.User;
import np.com.milapmagar.book.repository.BookRepository;
import np.com.milapmagar.book.repository.ReviewRepository;
import np.com.milapmagar.book.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    public ReviewService(ReviewRepository reviewRepository,
                         BookRepository bookRepository,
                         UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
    }

    public PageResponse<ReviewResponse> getReviews(Long bookId, int page, int size) {
        findBook(bookId); // a missing book is a 404, not an empty list
        return PageResponse.from(
                reviewRepository.findByBookId(bookId, Paging.newestFirst(page, size)).map(ReviewResponse::from));
    }

    public ReviewResponse getMyReview(Long bookId, Long userId) {
        return reviewRepository.findByBookIdAndUserId(bookId, userId)
                .map(ReviewResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("You have not reviewed this book yet"));
    }

    // Creates the caller's review, or changes it if there already is one ("upsert").
    @Transactional
    public ReviewResponse saveMyReview(Long bookId, Long userId, ReviewRequest request) {
        Book book = findBook(bookId);
        Review review = reviewRepository.findByBookIdAndUserId(bookId, userId)
                .orElseGet(() -> new Review(book, findUser(userId)));

        review.setRating(request.rating());
        review.setComment(request.comment() == null || request.comment().isBlank() ? null : request.comment().trim());
        reviewRepository.saveAndFlush(review);

        refreshRating(book);
        return ReviewResponse.from(review);
    }

    @Transactional
    public void deleteReview(Long reviewId, Long userId, boolean isAdmin) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review with id " + reviewId + " not found"));

        if (!isAdmin && !review.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You can only delete your own review");
        }

        Book book = review.getBook();
        reviewRepository.delete(review);
        reviewRepository.flush();
        refreshRating(book);
    }

    // Recomputes the book's average and count inside the same transaction as the review change,
    // so the two can never drift apart.
    private void refreshRating(Book book) {
        double average = reviewRepository.averageRating(book.getId());
        long count = reviewRepository.countByBookId(book.getId());
        book.setRating(Math.round(average * 100) / 100.0, (int) count);
    }

    private Book findBook(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book with id " + id + " not found"));
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + id + " not found"));
    }
}
