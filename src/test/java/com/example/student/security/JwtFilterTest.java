package com.example.student.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private JwtService jwtService;
    @InjectMocks
    private JwtFilter jwtFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain chain;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        request = new MockHttpServletRequest("GET", "/api/v1/student");
        request.setRequestURI("/api/v1/student");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private UserDetails admin() {
        return User.withUsername("admin@example.com").password("pass").authorities("ROLE_ADMIN").build();
    }

    @Test
    void withoutAuthorizationHeader_shouldPassRequestFurther() throws Exception {
        jwtFilter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtService);
    }

    @Test
    void withValidToken_shouldAuthenticateUser() throws Exception {
        request.addHeader("Authorization", "Bearer good-token");
        UserDetails admin = admin();
        when(jwtService.extractUsername("good-token")).thenReturn("admin@example.com");
        when(userDetailsService.loadUserByUsername("admin@example.com")).thenReturn(admin);
        when(jwtService.validateToken("good-token", admin)).thenReturn(true);

        jwtFilter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                .isEqualTo("admin@example.com");
    }

    @Test
    void withTokenThatFailsValidation_shouldNotAuthenticate() throws Exception {
        request.addHeader("Authorization", "Bearer bad-token");
        UserDetails admin = admin();
        when(jwtService.extractUsername("bad-token")).thenReturn("admin@example.com");
        when(userDetailsService.loadUserByUsername("admin@example.com")).thenReturn(admin);
        when(jwtService.validateToken("bad-token", admin)).thenReturn(false);

        jwtFilter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void withBrokenTokenOnApi_shouldReturn401() throws Exception {
        request.addHeader("Authorization", "Bearer broken");
        when(jwtService.extractUsername("broken")).thenThrow(new IllegalArgumentException("bad token"));

        jwtFilter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("IllegalArgumentException");
        assertThat(chain.getRequest()).isNull();
    }

}
