package np.com.milapmagar.book.dto;

public record AdminStatsResponse(
        long users,
        long books,
        ResourceCounts resources,
        long downloads
) {
    public record ResourceCounts(long pending, long approved, long rejected) {}
}
