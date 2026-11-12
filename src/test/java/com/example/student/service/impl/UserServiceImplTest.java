package com.example.student.service.impl;

import com.example.student.dto.user.UserRequest;
import com.example.student.dto.user.UserResponse;
import com.example.student.dto.user.UserResponseList;
import com.example.student.exception.EmailAlreadyExistsException;
import com.example.student.exception.EntityNotFoundException;
import com.example.student.mapper.UserMapper;
import com.example.student.model.Role;
import com.example.student.model.User;
import com.example.student.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private UserServiceImpl userService;

    private User createUser(String email, String password) {
        User user = new User();
        user.setId(1L);
        user.setEmail(email);
        user.setPassword(password);
        user.setRoles(Set.of(Role.ADMIN));
        return user;
    }

    //register

    @Test
    void register_whenEmailIsNull_shouldReturnFalse() {
        UserRequest request = new UserRequest();
        when(userMapper.toEntity(request)).thenReturn(createUser(null, "pass"));

        assertThat(userService.register(request)).isFalse();
        verifyNoInteractions(userRepository);
    }

    @Test
    void register_whenEmailAlreadyExists_shouldReturnFalse() {
        UserRequest request = new UserRequest("a@mail.ru", "pass", Set.of(Role.ADMIN));
        when(userMapper.toEntity(request)).thenReturn(createUser("a@mail.ru", "pass"));
        when(userRepository.existsByEmail("a@mail.ru")).thenReturn(true);

        assertThat(userService.register(request)).isFalse();
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_withPassword_shouldEncodePasswordAndSave() {
        UserRequest request = new UserRequest("a@mail.ru", "pass", Set.of(Role.ADMIN));
        User user = createUser("a@mail.ru", "pass");
        when(userMapper.toEntity(request)).thenReturn(user);
        when(userRepository.existsByEmail("a@mail.ru")).thenReturn(false);
        when(passwordEncoder.encode("pass")).thenReturn("encoded");

        assertThat(userService.register(request)).isTrue();
        assertThat(user.getPassword()).isEqualTo("encoded");
        verify(userRepository).save(user);
    }

    @Test
    void register_withBlankPassword_shouldSaveWithoutEncoding() {
        UserRequest request = new UserRequest("a@mail.ru", "  ", Set.of(Role.ADMIN));
        User user = createUser("a@mail.ru", "  ");
        when(userMapper.toEntity(request)).thenReturn(user);
        when(userRepository.existsByEmail("a@mail.ru")).thenReturn(false);

        assertThat(userService.register(request)).isTrue();
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository).save(user);
    }

    //finbyemail и findall

    @Test
    void findByEmail_whenUserExists_shouldReturnUser() {
        User user = createUser("a@mail.ru", "pass");
        when(userRepository.findByEmail("a@mail.ru")).thenReturn(Optional.of(user));

        assertThat(userService.findByEmail("a@mail.ru")).isSameAs(user);
    }

    @Test
    void findByEmail_whenUserNotFound_shouldThrowException() {
        when(userRepository.findByEmail("no@mail.ru")).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> userService.findByEmail("no@mail.ru"));
        assertThat(ex.getMessage()).contains("no@mail.ru");
    }

    //save

    @Test
    void save_whenEmailExists_shouldThrowException() {
        UserRequest request = new UserRequest("a@mail.ru", "pass", Set.of(Role.ADMIN));
        when(userRepository.existsByEmail("a@mail.ru")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> userService.save(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void save_whenEmailIsNew_shouldEncodePasswordAndSave() {
        UserRequest request = new UserRequest("new@mail.ru", "pass", Set.of(Role.TEACHER));
        UserResponse expected = new UserResponse(2L, "new@mail.ru", Set.of(Role.TEACHER));
        when(userRepository.existsByEmail("new@mail.ru")).thenReturn(false);
        when(passwordEncoder.encode("pass")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenReturn(expected);

        UserResponse result = userService.save(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("new@mail.ru");
        assertThat(saved.getPassword()).isEqualTo("encoded");
        assertThat(saved.getRoles()).containsExactly(Role.TEACHER);
        assertThat(result).isSameAs(expected);
    }
}