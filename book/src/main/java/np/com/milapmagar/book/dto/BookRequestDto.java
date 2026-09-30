package np.com.milapmagar.book.dto;

public record BookRequestDto(
        String title,
        String author,
        String publisher,
        String publishedYear,
        String isbn,
        Double price
) {
    /* Basically it is a response format for client which is w/o Id
    * {
    *   "title": "how to learn java",
    *   "author": "Nick Vandik",
    *   "publisher" : "Oreily's",
    *   "publishedYear" : "2024",
    *   "isbn" : "123123123123-123123",
    * }
    * */
}
