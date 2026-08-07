package com.yevos.timetracker.service;

import com.yevos.timetracker.model.dto.request.LoginRequest;
import com.yevos.timetracker.model.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse login(LoginRequest request);
}
