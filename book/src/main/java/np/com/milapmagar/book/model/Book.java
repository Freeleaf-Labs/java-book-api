package np.com.milapmagar.book.model;

import jakarta.persistence.*;

@Entity
@Table(name = "book")
public class Book {

    /** FIELDS **/
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // this above generative value generates id automatically
    @Column(name = "id")
    private Long id;

    @Column(name = "title")
    private String title;

    @Column(name = "author")
    private String author;

    @Column(name = "publisher")
    private String publisher;

    @Column(name = "publishedYear")
    private String publishedYear;

    @Column(name = "ISBN", unique = true, nullable = false)
    private String isbn;

    /** CONSTRUCTORS **/
    public Book() {
    }

    public Book(String title, String author, String publisher, String publishedYear, String isbn) {
        this.title = title;
        this.author = author;
        this.publisher = publisher;
        this.publishedYear = publishedYear;
        this.isbn = isbn;
    }

    /** GETTERS & SETTERS **/

    // ID
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
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
    public String getPublishedYear() {
        return publishedYear;
    }
    public void setPublishedYear(String publishedYear) {
        this.publishedYear = publishedYear;
    }

    // ISBN
    public String getIsbn() {
        return isbn;
    }
    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    @Override
    public String toString() {
        return "Book {" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", publisher='" + publisher + '\'' +
                ", publishedYear='" + publishedYear + '\'' +
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
