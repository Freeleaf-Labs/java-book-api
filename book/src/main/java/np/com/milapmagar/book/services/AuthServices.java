package np.com.milapmagar.book.services;

import np.com.milapmagar.book.dto.LoginRequestDto;
import np.com.milapmagar.book.repository.AuthRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthServices {

    private final AuthRepository authRepository;

    public AuthServices(AuthRepository authRepository){
        this.authRepository = authRepository;
    }

}
