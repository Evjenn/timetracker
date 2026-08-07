package com.yevos.timetracker.security.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.yevos.timetracker.model.entity.Role;
import com.yevos.timetracker.model.entity.UserEntity;
import com.yevos.timetracker.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    @DisplayName("Should successfully load user details when username exists in database")
    void loadUserByUsername_UserExists_ReturnsPopulatedUserDetails() {

        UserEntity userEntity = new UserEntity();
        userEntity.setId(1L);
        userEntity.setUsername("taras");
        userEntity.setPassword("encoded_password");
        userEntity.setEnabled(true);
        userEntity.setRole(Role.USER);

        when(userRepository.findByUsername("taras")).thenReturn(Optional.of(userEntity));
        UserDetails result = userDetailsService.loadUserByUsername("taras");

        assertNotNull(result);
        assertEquals("taras", result.getUsername());
    }

    @Test
    @DisplayName("Should throw UsernameNotFoundException when username does not exist in database")
    void loadUserByUsername_UserDoesNotExist_ThrowsUsernameNotFoundException() {

        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());
        assertThrows(UsernameNotFoundException.class, () -> {
            userDetailsService.loadUserByUsername("unknown");
        });
    }
}
