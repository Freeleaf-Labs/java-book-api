package np.com.milapmagar.book.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import np.com.milapmagar.book.config.OpenApiConfig;
import np.com.milapmagar.book.dto.PageResponse;
import np.com.milapmagar.book.dto.ReviewRequest;
import np.com.milapmagar.book.dto.ReviewResponse;
import np.com.milapmagar.book.security.CurrentUser;
import np.com.milapmagar.book.services.ReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Reviews", description = "Anyone can read reviews. Writing one needs a login; each user has one review per book.")
@RestController
@RequestMapping("/api/v1")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @Operation(summary = "List a book's reviews, newest first")
    @GetMapping("/books/{bookId}/reviews")
    public ResponseEntity<PageResponse<ReviewResponse>> getReviews(
            @PathVariable Long bookId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(reviewService.getReviews(bookId, page, size));
    }

    // The Authentication parameter is filled in by Spring Security from the verified token.
    @Operation(summary = "Get my review of a book", description = "404 when you have not reviewed it yet.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER))
    @GetMapping("/books/{bookId}/reviews/mine")
    public ResponseEntity<ReviewResponse> getMyReview(@PathVariable Long bookId, Authentication authentication) {
        return ResponseEntity.ok(reviewService.getMyReview(bookId, CurrentUser.id(authentication)));
    }

    @Operation(summary = "Create or change my review of a book", security = @SecurityRequirement(name = OpenApiConfig.BEARER))
    @PutMapping("/books/{bookId}/reviews/mine")
    public ResponseEntity<ReviewResponse> saveMyReview(
            @PathVariable Long bookId,
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(reviewService.saveMyReview(bookId, CurrentUser.id(authentication), request));
    }

    @Operation(summary = "Delete a review", description = "Your own review, or any review if you are an admin.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER))
    @DeleteMapping("/reviews/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id, Authentication authentication) {
        reviewService.deleteReview(id, CurrentUser.id(authentication), CurrentUser.isAdmin(authentication));
        return ResponseEntity.noContent().build();
    }
}
