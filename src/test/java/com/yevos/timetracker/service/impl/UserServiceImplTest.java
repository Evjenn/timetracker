package com.yevos.timetracker.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yevos.timetracker.exception.BaseException;
import com.yevos.timetracker.model.dto.request.RegisterRequest;
import com.yevos.timetracker.model.dto.request.UpdatePasswordRequest;
import com.yevos.timetracker.model.entity.UserEntity;
import com.yevos.timetracker.repository.UserRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private UserServiceImpl userService;
    private RegisterRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new RegisterRequest();
        validRequest.setUsername("taras_dev");
        validRequest.setPassword("RawPassword123");
        validRequest.setEmail("taras@example.com");
        validRequest.setHourlyRate(BigDecimal.valueOf(25.00));
    }

    @Test
    @DisplayName("Should successfully register a new user")
    void registerUser_Success() {
        // Given
        when(userRepository.existsByUsername("taras_dev")).thenReturn(false);
        when(passwordEncoder.encode("RawPassword123")).thenReturn("encodedPasswordString");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(
                invocation -> invocation.getArgument(0));
        // When
        userService.registerUser(validRequest);
        // Then
        verify(userRepository, times(1)).existsByUsername("taras_dev");
        verify(passwordEncoder, times(1)).encode("RawPassword123");
        verify(userRepository, times(1)).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("Should throw exception when registering if username is already taken")
    void registerUser_UsernameAlreadyExists_ThrowsException() {
        // Given
        when(userRepository.existsByUsername("taras_dev")).thenReturn(true);
        // When & Then
        BaseException exception = assertThrows(BaseException.class, () -> {
            userService.registerUser(validRequest);
        });

        assertEquals("Username 'taras_dev' is already taken", exception.getMessage());
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("Should successfully update user hourly rate")
    void updateHourlyRate_Success() {
        // Given
        UserEntity existingUser = new UserEntity();
        existingUser.setId(1L);
        existingUser.setUsername("taras_dev");
        existingUser.setHourlyRate(BigDecimal.valueOf(20.00));
        BigDecimal newRate = BigDecimal.valueOf(35.50);

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(
                invocation -> invocation.getArgument(0));
        // When
        UserEntity updatedUser = userService.updateHourlyRate(1L, newRate);
        // Then
        assertNotNull(updatedUser);
        assertEquals(newRate, updatedUser.getHourlyRate());
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).save(existingUser);
    }

    @Test
    @DisplayName("Should throw exception when updating rate if user does not exist")
    void updateHourlyRate_UserNotFound_ThrowsException() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        // When & Then
        BaseException exception = assertThrows(BaseException.class, () -> {
            userService.updateHourlyRate(1L, BigDecimal.valueOf(35.50));
        });

        assertEquals("User not found", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("Should successfully update user profile fields")
    void updateProfile_Success() {
        // Given
        UserEntity existingUser = new UserEntity();
        existingUser.setId(1L);
        existingUser.setUsername("old_name");
        existingUser.setEmail("old@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByUsername("new_name")).thenReturn(false);
        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        // When
        UserEntity updatedUser = userService
                .updateProfile(1L, "new_name", "new@example.com");
        // Then
        assertNotNull(updatedUser);
        assertEquals("new_name", updatedUser.getUsername());
        assertEquals("new@example.com", updatedUser.getEmail());
        verify(userRepository, times(1)).save(existingUser);
    }

    @Test
    @DisplayName("Should successfully update password when old password matches")
    void updatePassword_Success() {
        // Given
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setPassword("encodedOldPassword");

        UpdatePasswordRequest request = new UpdatePasswordRequest();
        request.setOldPassword("rawOldPassword");
        request.setNewPassword("rawNewPassword");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("rawOldPassword", "encodedOldPassword"))
                .thenReturn(true);
        when(passwordEncoder.encode("rawNewPassword"))
                .thenReturn("encodedNewPassword");
        // When
        userService.updatePassword(1L, request);
        // Then
        verify(userRepository, times(1)).save(user);
        assertEquals("encodedNewPassword", user.getPassword());
    }

    @Test
    @DisplayName("Should successfully deactivate user account by setting enabled to false")
    void deactivateUser_Success() {
        // Given
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setUsername("taras_dev");
        user.setEnabled(true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(
                invocation -> invocation.getArgument(0));
        // When
        userService.deactivateUser(1L);
        // Then
        assertFalse(user.isEnabled());
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).save(user);
    }
}
