package com.yevos.timetracker.mapper;

import com.yevos.timetracker.model.dto.response.UserResponse;
import com.yevos.timetracker.model.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(UserEntity user) {
        if (user == null) {
            return null;
        }

        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setHourlyRate(user.getHourlyRate());

        return response;
    }
}
