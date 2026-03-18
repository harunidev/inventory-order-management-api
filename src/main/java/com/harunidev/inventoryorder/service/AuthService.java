package com.harunidev.inventoryorder.service;

import com.harunidev.inventoryorder.dto.request.LoginRequest;
import com.harunidev.inventoryorder.dto.request.RegisterRequest;
import com.harunidev.inventoryorder.dto.response.AuthResponse;
import com.harunidev.inventoryorder.entity.RefreshToken;
import com.harunidev.inventoryorder.entity.Role;
import com.harunidev.inventoryorder.entity.User;
import com.harunidev.inventoryorder.exception.ResourceNotFoundException;
import com.harunidev.inventoryorder.repository.UserRepository;
import com.harunidev.inventoryorder.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalStateException("Username '" + request.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("Email '" + request.getEmail() + "' is already registered");
        }

        Role role = (request.getRole() != null) ? request.getRole() : Role.USER;

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .build();

        userRepository.save(user);
        String token = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .role(user.getRole().name())
                .expiresIn(jwtService.getExpiration())
                .refreshToken(refreshToken.getToken())
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.getUsername()));

        String token = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .role(user.getRole().name())
                .expiresIn(jwtService.getExpiration())
                .refreshToken(refreshToken.getToken())
                .build();
    }

    @Transactional
    public AuthResponse refreshAccessToken(String rawRefreshToken) {
        RefreshToken refreshToken = refreshTokenService.verifyAndGet(rawRefreshToken);
        User user = refreshToken.getUser();
        String newAccessToken = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(newAccessToken)
                .username(user.getUsername())
                .role(user.getRole().name())
                .expiresIn(jwtService.getExpiration())
                .refreshToken(rawRefreshToken)
                .build();
    }

    @Transactional
    public void logout(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        refreshTokenService.revokeByUser(user);
    }
}
