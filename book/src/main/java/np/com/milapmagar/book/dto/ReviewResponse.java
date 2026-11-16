package np.com.milapmagar.book.dto;

import np.com.milapmagar.book.model.Review;

import java.time.Instant;

public record ReviewResponse(
        Long id,
        Long bookId,
        int rating,
        String comment,
        Reviewer user,
        Instant createdAt,
        Instant updatedAt
) {
    // only what is safe to show next to a public review: never the email
    public record Reviewer(Long id, String displayName) {}

    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getBook().getId(),
                review.getRating(),
                review.getComment(),
                new Reviewer(review.getUser().getId(), review.getUser().getDisplayName()),
                review.getCreatedAt(),
                review.getUpdatedAt());
    }
}
