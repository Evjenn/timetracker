package com.yevos.timetracker.security.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class UserDetailsImplTest {

    @Test
    @DisplayName("Should correctly return initialized fields and standard security properties for active user")
    void getProperties_ActiveUser_ReturnsCorrectFieldsAndDefaultTrueFlags() {

        UserDetailsImpl user = new UserDetailsImpl(
                1L,
                "taras",
                "password",
                true,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        assertEquals(1L, user.getId());
        assertEquals("taras", user.getUsername());
        assertEquals("password", user.getPassword());
        assertTrue(user.isAccountNonExpired());
        assertTrue(user.isAccountNonLocked());
        assertTrue(user.isCredentialsNonExpired());
        assertTrue(user.isEnabled());
    }
}
