package com.yevos.timetracker.controller;

import com.yevos.timetracker.model.dto.request.LoginRequest;
import com.yevos.timetracker.model.dto.request.RegisterRequest;
import com.yevos.timetracker.model.dto.response.AuthResponse;
import com.yevos.timetracker.service.AuthService;
import com.yevos.timetracker.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Validated
public class AuthControllerV1 {

    private final AuthService authService;
    private final UserService userService;

    public AuthControllerV1(AuthService authService,
                            UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(
            @Valid @RequestBody RegisterRequest request) {

        userService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    @Operation(
            summary = "Authenticate user and return JWT token",
            description = """
                    * **ADMIN:** Username: `super_admin` | Password: `TimetrackerAdmin123`
                    * **USER:** Username: `taras_dev` | Password: `RawPassword123`
                    """
    )
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(authService.login(request));
    }
}
