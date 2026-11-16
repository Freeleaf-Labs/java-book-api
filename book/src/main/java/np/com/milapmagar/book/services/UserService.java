package np.com.milapmagar.book.services;

import np.com.milapmagar.book.dto.AdminStatsResponse;
import np.com.milapmagar.book.dto.AdminUserUpdateRequest;
import np.com.milapmagar.book.dto.PageResponse;
import np.com.milapmagar.book.dto.UpdateProfileRequest;
import np.com.milapmagar.book.dto.UserResponse;
import np.com.milapmagar.book.exception.ConflictException;
import np.com.milapmagar.book.exception.ResourceNotFoundException;
import np.com.milapmagar.book.model.User;
import np.com.milapmagar.book.repository.BookRepository;
import np.com.milapmagar.book.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    public UserService(UserRepository userRepository, BookRepository bookRepository) {
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
    }

    /** THE LOGGED-IN USER **/

    public UserResponse getUser(Long id) {
        return UserResponse.from(findUser(id));
    }

    @Transactional
    public UserResponse updateProfile(Long id, UpdateProfileRequest request) {
        User user = findUser(id);
        user.setDisplayName(request.displayName().trim());
        return UserResponse.from(user); // no save(): the change is written when the transaction commits
    }

    /** ADMIN **/

    public PageResponse<UserResponse> getUsers(String search, int page, int size) {
        Pageable pageable = Paging.newestFirst(page, size);
        Page<User> users = search == null || search.isBlank()
                ? userRepository.findAll(pageable)
                : userRepository.findByEmailContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(
                        search.trim(), search.trim(), pageable);
        return PageResponse.from(users.map(UserResponse::from));
    }

    @Transactional
    public UserResponse updateUser(Long id, AdminUserUpdateRequest request, Long actingAdminId) {
        // Otherwise the last admin could lock everyone, themselves included, out of the admin area.
        if (id.equals(actingAdminId)) {
            throw new ConflictException("You cannot change your own role or disable your own account");
        }

        User user = findUser(id);
        if (request.role() != null) user.setRole(request.role());
        if (request.enabled() != null) user.setEnabled(request.enabled());
        return UserResponse.from(user);
    }

    public AdminStatsResponse getStats() {
        // Uploads and downloads are not built yet, so those numbers are honest zeros.
        return new AdminStatsResponse(
                userRepository.count(),
                bookRepository.count(),
                new AdminStatsResponse.ResourceCounts(0, 0, 0),
                0);
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + id + " not found"));
    }
}
