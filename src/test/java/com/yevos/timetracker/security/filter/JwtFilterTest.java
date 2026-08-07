package com.yevos.timetracker.security.filter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.yevos.timetracker.security.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;
    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private JwtFilter jwtFilter;

    @Test
    @DisplayName("Should pass request through filter chain without interactions when Authorization header is missing")
    void doFilterInternal_AuthorizationHeaderMissing_SkipsFilterAndContinuesChain() throws Exception {

        when(request.getHeader("Authorization")).thenReturn(null);

        jwtFilter.doFilter(request, response, filterChain);
        verify(filterChain, times(1)).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("Should successfully authenticate user when Bearer token is fully valid")
    void doFilterInternal_ValidBearerToken_AuthenticatesUserAndContinuesChain() throws Exception {

        String mockToken = "valid.jwt.token";
        String username = "john_doe";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + mockToken);
        when(jwtService.extractUsername(mockToken)).thenReturn(username);
        when(jwtService.isValid(mockToken)).thenReturn(true);

        UserDetails mockDetails = mock(UserDetails.class);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(mockDetails);
        jwtFilter.doFilter(request, response, filterChain);

        verify(jwtService, times(1)).extractUsername(mockToken);
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Should skip authentication when Bearer token is expired or broken")
    void doFilterInternal_InvalidBearerToken_SkipsAuthenticationAndContinuesChain() throws Exception {

        String mockToken = "expired.jwt.token";
        String username = "john_doe";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + mockToken);
        when(jwtService.extractUsername(mockToken)).thenReturn(username);
        when(jwtService.isValid(mockToken)).thenReturn(false);

        jwtFilter.doFilter(request, response, filterChain);
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Should pass request through filter chain without interactions when Authorization header format is not Bearer")
    void doFilterInternal_InvalidHeaderPrefix_SkipsFilterAndContinuesChain() throws Exception {

        when(request.getHeader("Authorization")).thenReturn("Basic c29tZXRva2Vu");

        jwtFilter.doFilter(request, response, filterChain);
        verify(filterChain, times(1)).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }
}
