package com.harunidev.inventoryorder.service;

import com.harunidev.inventoryorder.dto.request.LoginRequest;
import com.harunidev.inventoryorder.dto.request.RegisterRequest;
import com.harunidev.inventoryorder.dto.response.AuthResponse;
import com.harunidev.inventoryorder.entity.RefreshToken;
import com.harunidev.inventoryorder.entity.Role;
import com.harunidev.inventoryorder.entity.User;
import com.harunidev.inventoryorder.repository.UserRepository;
import com.harunidev.inventoryorder.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private User savedUser;
    private RefreshToken refreshToken;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setEmail("new@test.com");
        registerRequest.setPassword("password123");

        savedUser = User.builder()
                .id(1L)
                .username("newuser")
                .email("new@test.com")
                .password("encoded_password")
                .role(Role.USER)
                .build();

        refreshToken = RefreshToken.builder()
                .id(1L)
                .token("refresh-uuid-token")
                .user(savedUser)
                .expiryDate(Instant.now().plusSeconds(604800))
                .build();
    }

    // ─── register ─────────────────────────────────────────────────────────────

    @Test
    void register_success_defaultRoleIsUser() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");
        when(jwtService.getExpiration()).thenReturn(86400000L);
        when(refreshTokenService.createRefreshToken(any(User.class))).thenReturn(refreshToken);

        AuthResponse response = authService.register(registerRequest);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUsername()).isEqualTo("newuser");
        assertThat(response.getRole()).isEqualTo("USER");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-uuid-token");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
        verify(refreshTokenService).createRefreshToken(any(User.class));
    }

    @Test
    void register_withAdminRole_setsAdminRole() {
        registerRequest.setRole(Role.ADMIN);

        User adminUser = User.builder()
                .id(2L)
                .username("newuser")
                .email("new@test.com")
                .password("encoded_password")
                .role(Role.ADMIN)
                .build();

        RefreshToken adminRefreshToken = RefreshToken.builder()
                .token("admin-refresh-token")
                .user(adminUser)
                .expiryDate(Instant.now().plusSeconds(604800))
                .build();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(adminUser);
        when(jwtService.generateToken(any(User.class))).thenReturn("admin-token");
        when(jwtService.getExpiration()).thenReturn(86400000L);
        when(refreshTokenService.createRefreshToken(any(User.class))).thenReturn(adminRefreshToken);

        AuthResponse response = authService.register(registerRequest);

        assertThat(response.getRole()).isEqualTo("ADMIN");
    }

    @Test
    void register_duplicateUsername_throwsException() {
        when(userRepository.existsByUsername("newuser")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("newuser")
                .hasMessageContaining("already taken");

        verify(userRepository, never()).save(any());
        verify(refreshTokenService, never()).createRefreshToken(any());
    }

    @Test
    void register_duplicateEmail_throwsException() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("new@test.com")
                .hasMessageContaining("already registered");

        verify(userRepository, never()).save(any());
    }

    // ─── login ────────────────────────────────────────────────────────────────

    @Test
    void login_success() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("newuser");
        loginRequest.setPassword("password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.of(savedUser));
        when(jwtService.generateToken(savedUser)).thenReturn("jwt-token");
        when(jwtService.getExpiration()).thenReturn(86400000L);
        when(refreshTokenService.createRefreshToken(savedUser)).thenReturn(refreshToken);

        AuthResponse response = authService.login(loginRequest);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUsername()).isEqualTo("newuser");
        assertThat(response.getRole()).isEqualTo("USER");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-uuid-token");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(refreshTokenService).createRefreshToken(savedUser);
    }

    @Test
    void login_userNotFound_throwsException() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("ghost");
        loginRequest.setPassword("password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(RuntimeException.class);
    }

    // ─── refreshAccessToken ───────────────────────────────────────────────────

    @Test
    void refreshAccessToken_validToken_returnsNewAccessToken() {
        when(refreshTokenService.verifyAndGet("refresh-uuid-token")).thenReturn(refreshToken);
        when(jwtService.generateToken(savedUser)).thenReturn("new-jwt-token");
        when(jwtService.getExpiration()).thenReturn(86400000L);

        AuthResponse response = authService.refreshAccessToken("refresh-uuid-token");

        assertThat(response.getToken()).isEqualTo("new-jwt-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-uuid-token");
        assertThat(response.getUsername()).isEqualTo("newuser");
    }

    @Test
    void refreshAccessToken_invalidToken_throwsException() {
        when(refreshTokenService.verifyAndGet("bad-token"))
                .thenThrow(new RuntimeException("Refresh token not found or already revoked"));

        assertThatThrownBy(() -> authService.refreshAccessToken("bad-token"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found or already revoked");
    }

    // ─── logout ───────────────────────────────────────────────────────────────

    @Test
    void logout_revokesRefreshToken() {
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.of(savedUser));

        authService.logout("newuser");

        verify(refreshTokenService).revokeByUser(savedUser);
    }
}
