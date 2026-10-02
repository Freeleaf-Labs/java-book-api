package np.com.milapmagar.book.services;

import np.com.milapmagar.book.dto.BookResponseDto;
import np.com.milapmagar.book.exception.ResourceNotFoundException;
import np.com.milapmagar.book.repository.BookRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public List<BookResponseDto> getBooks(){
        return bookRepository.findAll().stream()
                .map(BookResponseDto::from)
                .toList();
    }

    public BookResponseDto getBookById(Long id){
        return bookRepository.findById(id)
                .map(BookResponseDto::from)
                .orElseThrow(() -> new ResourceNotFoundException("Book with id " + id + " not found"));
    }
}
