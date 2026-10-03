package np.com.milapmagar.book.dto;

import np.com.milapmagar.book.model.Book;
import np.com.milapmagar.book.model.User;

public record LoginRequestDto(
        Long id,
        String email,
        String password
) {
    // the one place an entity is turned into a response
    public static LoginRequestDto from(User user){
        return new LoginRequestDto(
                user.getEmail(),
                user.getPassword()
        )
    }
}
