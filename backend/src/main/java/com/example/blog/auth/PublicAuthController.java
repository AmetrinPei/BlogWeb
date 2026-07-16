package com.example.blog.auth;

import com.example.blog.common.ClientIpResolver;
import com.example.blog.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class PublicAuthController {

    private final AuthService authService;

    public PublicAuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.ok(authService.register(request));
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.ok(authService.login(request, ClientIpResolver.resolve(httpRequest)));
    }

    @PostMapping("/refresh")
    public Result<TokenPairResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return Result.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public Result<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return Result.ok(null);
    }
}
