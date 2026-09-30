package np.com.milapmagar.book.services;

import np.com.milapmagar.book.dto.BookResponseDto;
import np.com.milapmagar.book.model.Book;
import np.com.milapmagar.book.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookService {

    @Autowired
    private BookRepository bookRepository;

    public List<BookResponseDto> getBooks(){
        return bookRepository.findAll().stream()
                .map(book -> new BookResponseDto(book.getId(), book.getTitle(), book.getAuthor(), book.getPublisher(), book.getPublishedYear(), book.getIsbn(), book.getPrice()))
                .toList();
    }

    public BookResponseDto getBookById(Long id){
        Book book = bookRepository.findById(id).orElseThrow(() -> new RuntimeException("Book not found"));
        // returning the book value
        return new BookResponseDto(book.getId(), book.getTitle(), book.getAuthor(), book.getPublisher(), book.getPublishedYear(), book.getIsbn(), book.getPrice());
    }
}