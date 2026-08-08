package com.yevos.timetracker.controller;

import com.yevos.timetracker.mapper.UserMapper;
import com.yevos.timetracker.model.dto.request.UpdatePasswordRequest;
import com.yevos.timetracker.model.dto.request.UpdateRateRequest;
import com.yevos.timetracker.model.dto.request.UpdateUserRequest;
import com.yevos.timetracker.model.dto.response.UserResponse;
import com.yevos.timetracker.model.entity.UserEntity;
import com.yevos.timetracker.security.service.UserDetailsImpl;
import com.yevos.timetracker.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Validated
public class UserControllerV1 {

    private final UserService userService;
    private final UserMapper userMapper;

    public UserControllerV1(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @PutMapping("/rate")
    @Operation(summary = "Update user hourly rate for profit calculations")
    public ResponseEntity<UserResponse> updateHourlyRate(
            @AuthenticationPrincipal UserDetailsImpl userPrincipal,
            @Valid @RequestBody UpdateRateRequest request) {

        UserEntity updatedUser = userService.updateHourlyRate(userPrincipal.getId(),
                request.getHourlyRate());
        return ResponseEntity.ok(userMapper.toResponse(updatedUser));
    }

    @PatchMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(
            @AuthenticationPrincipal UserDetailsImpl userPrincipal,
            @Valid @RequestBody UpdateUserRequest request) {

        UserEntity updatedUser = userService.updateProfile(
                userPrincipal.getId(),
                request.getUsername(),
                request.getEmail()
        );

        return ResponseEntity.ok(userMapper.toResponse(updatedUser));
    }

    @PutMapping("/password")
    public ResponseEntity<Void> updatePassword(
            @AuthenticationPrincipal UserDetailsImpl userPrincipal,
            @Valid @RequestBody UpdatePasswordRequest request) {

        userService.updatePassword(userPrincipal.getId(), request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(
            @AuthenticationPrincipal UserDetailsImpl userPrincipal) {

        userService.deactivateUser(userPrincipal.getId());
        return ResponseEntity.noContent().build(); // Возвращаем статус 204 No Content
    }
}
