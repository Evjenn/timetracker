package com.yevos.timetracker.service;

import com.yevos.timetracker.model.dto.request.RegisterRequest;
import com.yevos.timetracker.model.dto.request.UpdatePasswordRequest;
import com.yevos.timetracker.model.entity.UserEntity;
import java.math.BigDecimal;

public interface UserService {

    void registerUser(RegisterRequest request);

    UserEntity updateHourlyRate(Long userId, BigDecimal newRate);

    UserEntity updateProfile(Long userId, String newUsername, String newEmail);

    void updatePassword(Long userId, UpdatePasswordRequest request);

    void deactivateUser(Long userId);
}
