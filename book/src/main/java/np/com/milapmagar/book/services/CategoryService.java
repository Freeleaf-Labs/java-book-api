package np.com.milapmagar.book.services;

import np.com.milapmagar.book.dto.CategoryRequest;
import np.com.milapmagar.book.dto.CategoryResponse;
import np.com.milapmagar.book.exception.BadRequestException;
import np.com.milapmagar.book.exception.ConflictException;
import np.com.milapmagar.book.exception.ResourceNotFoundException;
import np.com.milapmagar.book.model.Category;
import np.com.milapmagar.book.repository.BookRepository;
import np.com.milapmagar.book.repository.CategoryRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final BookRepository bookRepository;

    public CategoryService(CategoryRepository categoryRepository, BookRepository bookRepository) {
        this.categoryRepository = categoryRepository;
        this.bookRepository = bookRepository;
    }

    public List<CategoryResponse> getCategories() {
        return categoryRepository.findAll(Sort.by("name")).stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        String name = request.name().trim();
        String slug = slugify(name);
        if (categoryRepository.existsBySlug(slug)) {
            throw new ConflictException("Category '" + name + "' already exists");
        }
        return CategoryResponse.from(categoryRepository.save(new Category(name, slug)));
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = findCategory(id);
        String name = request.name().trim();
        String slug = slugify(name);
        if (categoryRepository.existsBySlugAndIdNot(slug, id)) {
            throw new ConflictException("Category '" + name + "' already exists");
        }
        category.setName(name);
        category.setSlug(slug);
        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = findCategory(id);
        if (bookRepository.existsByCategoryId(id)) {
            throw new ConflictException("Category '" + category.getName() + "' still has books in it");
        }
        categoryRepository.delete(category);
    }

    private Category findCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category with id " + id + " not found"));
    }

    // "Computer Science" -> "computer-science". Letters and digits of any alphabet are kept.
    static String slugify(String name) {
        String slug = name.toLowerCase()
                .replaceAll("[^\\p{L}\\p{M}\\p{N}]+", "-")
                .replaceAll("^-|-$", "");
        if (slug.isEmpty()) {
            throw new BadRequestException("Name must contain at least one letter or digit");
        }
        return slug;
    }
}
