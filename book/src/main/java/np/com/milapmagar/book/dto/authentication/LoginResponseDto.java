package np.com.milapmagar.book.dto.authentication;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import np.com.milapmagar.book.model.User;

public record LoginResponseDto(
    Long id,
    String fullName,
    String email,
    String phone
) {
//    public static LoginResponseDto from(User user){
////        user.
//    }
}
