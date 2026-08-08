package com.yevos.timetracker.service.impl;

import com.yevos.timetracker.exception.BaseException;
import com.yevos.timetracker.model.dto.request.RegisterRequest;
import com.yevos.timetracker.model.dto.request.UpdatePasswordRequest;
import com.yevos.timetracker.model.entity.UserEntity;
import com.yevos.timetracker.repository.UserRepository;
import com.yevos.timetracker.service.UserService;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void registerUser(RegisterRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BaseException("Username '"
                    + request.getUsername() + "' is already taken", HttpStatus.CONFLICT);
        }
        UserEntity user = new UserEntity();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setHourlyRate(request.getHourlyRate());
        user.setEmail(request.getEmail());

        userRepository.save(user);

    }

    @Override
    @Transactional
    public UserEntity updateHourlyRate(Long userId, BigDecimal newRate) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException("User not found", HttpStatus.NOT_FOUND));

        user.setHourlyRate(newRate);
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public UserEntity updateProfile(Long userId, String newUsername, String newEmail) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException("User not found", HttpStatus.NOT_FOUND));

        if (newUsername != null && !newUsername.isBlank()
                && !newUsername.equals(user.getUsername())) {
            if (userRepository.existsByUsername(newUsername)) {
                throw new BaseException("Username '" + newUsername
                        + "' is already taken", HttpStatus.CONFLICT);
            }
            user.setUsername(newUsername);
        }
        if (newEmail != null && !newEmail.isBlank()) {
            user.setEmail(newEmail);
        }

        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void updatePassword(Long userId, UpdatePasswordRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException("User not found", HttpStatus.NOT_FOUND));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BaseException("Invalid old password", HttpStatus.BAD_REQUEST);
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deactivateUser(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException("User not found", HttpStatus.NOT_FOUND));

        user.setEnabled(false);
        userRepository.save(user);
    }
}
