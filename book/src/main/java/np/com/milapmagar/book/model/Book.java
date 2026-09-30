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
    private long id;

    @Column(name = "title")
    private String title;

    @Column(name = "author")
    private String author;

    @Column(name = "publisher")
    private String publisher;

    @Column(name = "publishedYear")
    private String publishedYear;

    @Column(name = "price")
    private Double price;

    @Column(name = "ISBN", unique = true, nullable = false)
    private String isbn;

    /** CONSTRUCTORS **/
    public Book() {
    }

    public Book(String title, String author, String publisher, String publishedYear, Double price, String isbn) {
        this.title = title;
        this.author = author;
        this.publisher = publisher;
        this.publishedYear = publishedYear;
        this.price = price;
        this.isbn = isbn;
    }

    /** GETTERS & SETTERS **/

    // ID
    public long getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    // TITLE (Added missing setter)
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

    // PRICE
    public Double getPrice() {
        return price;
    }
    public void setPrice(Double price) {
        this.price = price;
    }

    @Override
    public String toString() {
        return "Book {" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", publisher='" + publisher + '\'' +
                ", publishedYear='" + publishedYear + '\'' +
                ", price=" + price +
                ", isbn='" + isbn + '\'' +
                '}';
    }

    /** APPLY DISCOUNT**/
    public void applyDiscount(double percentage){
        if(percentage <= 0 || percentage > 100){
            throw new IllegalArgumentException("Discount percentage must be between 0 and 50.");
        }else if(this.price == null || this.price <= 0){
            throw new IllegalStateException("Cannot apply a discount to a book to no price");
        }

        double discountedAmount = this.price * (percentage / 100.00);
        this.price -= discountedAmount;
    }

    /** HASH code && EQUALS**/
    @Override
    public boolean equals(Object obj){
        if(this == obj) return true; //check as if it is the same physical book.
        if(obj == null || getClass() != obj.getClass()) return false; // check the book is even book or anything else
        Book book = (Book) obj;

        // two books are equal if their ISBNs match (&& ISBN is not null)
        return isbn != null && isbn.equals(book.isbn); // checking with isbn number as one particular book has one isbn number.
    }

    @Override
    public int hashCode(){
        // creating a fingerprint to find the book out once needed.
        return java.util.Objects.hashCode(isbn);
    }

    public Object getTui() {
        return null;
    }
}