package np.com.milapmagar.book.services;

import np.com.milapmagar.book.dto.authentication.LoginRequestDto;
import np.com.milapmagar.book.dto.authentication.LoginResponseDto;
import np.com.milapmagar.book.dto.authentication.RegisterRequestDto;
import np.com.milapmagar.book.dto.authentication.RegisterResponseDto;
import np.com.milapmagar.book.exception.EmailAlreadyExistsException;
import np.com.milapmagar.book.exception.InvalidCredentialsException;
import np.com.milapmagar.book.model.User;
import np.com.milapmagar.book.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServices {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthServices(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // reigster-api-services
    public RegisterResponseDto register(RegisterRequestDto registerRequest){

        // new user creation
        User user = new User();

        if (Boolean.parseBoolean(userRepository.existsByEmail(registerRequest.email()))) {
            throw new EmailAlreadyExistsException("Email is already registered");
        }

        user.setFullName(registerRequest.fullName());
        user.setEmail(registerRequest.email());
        user.setPhone(registerRequest.phone());

        // Hashing the password first then adding in the dashboard
        String hashedPassword = passwordEncoder.encode(registerRequest.password());
        // Setting the hashed password
        user.setPassword(hashedPassword);
        userRepository.save(user);
        // response
        return new RegisterResponseDto(
                user.getFullName(),
                user.getEmail(),
                user.getPhone()
        );
    }

    // login-api-services
    public LoginResponseDto login(LoginRequestDto loginRequestDto) {

        User user = userRepository
                .findByEmail(loginRequestDto.email())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        boolean matches = passwordEncoder.matches(
                loginRequestDto.password(),
                user.getPassword()
        );

        if (!matches) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        // response
        return new LoginResponseDto(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone()
        );
    }
}
