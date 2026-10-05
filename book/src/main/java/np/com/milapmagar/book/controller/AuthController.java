package np.com.milapmagar.book.controller;

import np.com.milapmagar.book.dto.authentication.LoginRequestDto;
import np.com.milapmagar.book.dto.authentication.LoginResponseDto;
import np.com.milapmagar.book.dto.authentication.RegisterRequestDto;
import np.com.milapmagar.book.dto.authentication.RegisterResponseDto;
import np.com.milapmagar.book.services.AuthServices;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthServices authServices;

    public AuthController(AuthServices authServices) {
        this.authServices = authServices;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(
            @RequestBody LoginRequestDto loginRequestDto
    ) {

        LoginResponseDto response = authServices.login(loginRequestDto);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDto> register(
            @RequestBody RegisterRequestDto registerRequestDto
    ){

        RegisterResponseDto response = authServices.register(registerRequestDto);

        return ResponseEntity.ok(response);
    }
}