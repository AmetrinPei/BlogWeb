package com.example.blog.user;

import com.example.blog.auth.CurrentUserService;
import com.example.blog.auth.RefreshTokenService;
import com.example.blog.common.BusinessException;
import com.example.blog.common.ErrorCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordChangeService {

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    public PasswordChangeService(
            CurrentUserService currentUserService,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            RefreshTokenService refreshTokenService
    ) {
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public ChangePasswordResponse changePassword(ChangePasswordRequest request) {
        User user = currentUserService.requireUser();

        String currentPassword = request.getCurrentPassword();
        String newPassword = request.getNewPassword();
        String confirmPassword = request.getConfirmPassword();

        if (!newPassword.equals(confirmPassword)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "两次输入的新密码不一致");
        }

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "当前密码不正确");
        }

        if (newPassword.equals(currentPassword)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "新密码不能与当前密码相同");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.saveAndFlush(user);
        refreshTokenService.revokeAllByUserId(user.getId());

        return new ChangePasswordResponse(user.getId(), user.getUsername());
    }
}
