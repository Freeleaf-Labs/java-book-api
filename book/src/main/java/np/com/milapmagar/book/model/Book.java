package np.com.milapmagar.book.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "books")
public class Book {

    /** FIELDS **/
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // this above generative value generates id automatically
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    private String publisher;

    private Integer publishedYear;

    // optional, but two books can never share one
    @Column(unique = true)
    private String isbn;

    @Column(length = 5000)
    private String description;

    // a plain link to a cover image; null when the book has none
    private String coverUrl;

    // LAZY: the category row is only loaded when something actually asks for it
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    // kept up to date by ReviewService every time a review is added, changed or removed
    @Column(nullable = false)
    private double ratingAvg;

    @Column(nullable = false)
    private int ratingCount;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    /** CONSTRUCTORS **/
    public Book() {
    }

    /** GETTERS & SETTERS **/

    // ID
    public Long getId() {
        return id;
    }

    // TITLE
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }

    // AUTHOR
    public String getAuthor() {
        return author;
    }
    public void setAuthor(String author) {
        this.author = author;
    }

    // PUBLISHER
    public String getPublisher() {
        return publisher;
    }
    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    // PUBLISHED YEAR
    public Integer getPublishedYear() {
        return publishedYear;
    }
    public void setPublishedYear(Integer publishedYear) {
        this.publishedYear = publishedYear;
    }

    // ISBN
    public String getIsbn() {
        return isbn;
    }
    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    // DESCRIPTION
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    // COVER
    public String getCoverUrl() {
        return coverUrl;
    }
    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    // CATEGORY
    public Category getCategory() {
        return category;
    }
    public void setCategory(Category category) {
        this.category = category;
    }

    // RATING
    public double getRatingAvg() {
        return ratingAvg;
    }
    public int getRatingCount() {
        return ratingCount;
    }
    public void setRating(double ratingAvg, int ratingCount) {
        this.ratingAvg = ratingAvg;
        this.ratingCount = ratingCount;
    }

    // TIMESTAMPS (set by Hibernate, so there are no setters)
    public Instant getCreatedAt() {
        return createdAt;
    }
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return "Book {" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", publisher='" + publisher + '\'' +
                ", publishedYear=" + publishedYear +
                ", isbn='" + isbn + '\'' +
                '}';
    }

    /** HASH code && EQUALS**/
    @Override
    public boolean equals(Object obj){
        if(this == obj) return true; //check as if it is the same physical book.
        if(obj == null || getClass() != obj.getClass()) return false; // check the book is even book or anything else
        Book book = (Book) obj;

        // two books are equal if they are the same database row; a book that is not saved yet (id == null) equals only itself.
        return id != null && id.equals(book.id);
    }

    @Override
    public int hashCode(){
        // constant per class, so the hash does not change when save() assigns the id.
        return getClass().hashCode();
    }
}
