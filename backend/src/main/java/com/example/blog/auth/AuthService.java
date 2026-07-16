package com.example.blog.auth;

import com.example.blog.common.BusinessException;
import com.example.blog.common.ErrorCode;
import com.example.blog.config.AuthProperties;
import com.example.blog.user.User;
import com.example.blog.user.UserRepository;
import com.example.blog.user.UserRole;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;
    private final AuthProperties authProperties;
    private final LoginRateLimitService loginRateLimitService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            JwtProperties jwtProperties,
            RefreshTokenService refreshTokenService,
            AuthProperties authProperties,
            LoginRateLimitService loginRateLimitService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.refreshTokenService = refreshTokenService;
        this.authProperties = authProperties;
        this.loginRateLimitService = loginRateLimitService;
    }

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (!authProperties.isPublicRegistrationEnabled()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "公开注册已关闭");
        }
        String username = request.getUsername().trim();
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.AUTHOR);
        userRepository.save(user);
        return toLoginResponse(user);
    }

    @Transactional
    public LoginResponse login(LoginRequest request, String clientIp) {
        String username = request.getUsername() == null ? "" : request.getUsername().trim();
        loginRateLimitService.assertNotBlocked(clientIp, username);
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误"));

            if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
            }

            loginRateLimitService.clear(clientIp, username);
            return toLoginResponse(user);
        } catch (BusinessException ex) {
            if (ex.getCode() == ErrorCode.UNAUTHORIZED) {
                loginRateLimitService.recordFailure(clientIp, username);
            }
            throw ex;
        }
    }

    @Transactional
    public TokenPairResponse refresh(RefreshRequest request) {
        String raw = request.getRefreshToken();
        RefreshToken current = refreshTokenService.requireActive(raw);
        User user = userRepository.findById(current.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "登录已失效，请重新登录"));

        String newRefresh = refreshTokenService.rotate(raw);
        String role = user.getRole() == null ? UserRole.AUTHOR.name() : user.getRole().name();
        String access = jwtService.createToken(user.getId(), user.getUsername(), role);
        return new TokenPairResponse(
                access,
                newRefresh,
                jwtProperties.getAccessExpireMinutes(),
                jwtProperties.compatExpireHours()
        );
    }

    @Transactional
    public void logout(LogoutRequest request) {
        refreshTokenService.revoke(request.getRefreshToken());
    }

    private LoginResponse toLoginResponse(User user) {
        String role = user.getRole() == null ? UserRole.AUTHOR.name() : user.getRole().name();
        String token = jwtService.createToken(user.getId(), user.getUsername(), role);
        String refreshToken = refreshTokenService.issue(user.getId());
        return new LoginResponse(
                token,
                refreshToken,
                jwtProperties.getAccessExpireMinutes(),
                jwtProperties.compatExpireHours(),
                user.getId(),
                user.getUsername(),
                role,
                user.getDisplayName(),
                user.getAvatarUrl()
        );
    }
}
