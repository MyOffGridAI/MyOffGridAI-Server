package com.myoffgridai.auth.service;

import com.myoffgridai.auth.dto.UserDetailDto;
import com.myoffgridai.auth.dto.UserSummaryDto;
import com.myoffgridai.auth.model.Role;
import com.myoffgridai.auth.model.User;
import com.myoffgridai.auth.repository.UserRepository;
import com.myoffgridai.common.exception.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
    }

    private User testUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testuser");
        user.setEmail("test@test.com");
        user.setDisplayName("Test User");
        user.setPasswordHash("hash");
        user.setRole(Role.ROLE_MEMBER);
        user.setIsActive(true);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        return user;
    }

    @Test
    void listUsers_shouldReturnPageOfSummaries() {
        User user = testUser();
        Page<User> page = new PageImpl<>(List.of(user));
        when(userRepository.findAll(any(PageRequest.class))).thenReturn(page);

        Page<UserSummaryDto> result = userService.listUsers(0, 20);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).username()).isEqualTo("testuser");
    }

    @Test
    void listUsers_shouldClampPageSize() {
        Page<User> page = new PageImpl<>(List.of());
        when(userRepository.findAll(PageRequest.of(0, 100))).thenReturn(page);

        userService.listUsers(0, 200); // Should be clamped to 100
        verify(userRepository).findAll(PageRequest.of(0, 100));
    }

    @Test
    void getUserById_found_shouldReturnDetail() {
        User user = testUser();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        UserDetailDto detail = userService.getUserById(user.getId());

        assertThat(detail.username()).isEqualTo("testuser");
        assertThat(detail.email()).isEqualTo("test@test.com");
    }

    @Test
    void getUserById_notFound_shouldThrow() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(id))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void updateUser_shouldUpdateFields() {
        User user = testUser();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserDetailDto updated = userService.updateUser(
                user.getId(), "New Name", "new@test.com", Role.ROLE_ADMIN);

        assertThat(user.getDisplayName()).isEqualTo("New Name");
        assertThat(user.getEmail()).isEqualTo("new@test.com");
        assertThat(user.getRole()).isEqualTo(Role.ROLE_ADMIN);
    }

    @Test
    void updateUser_withNullFields_shouldNotChange() {
        User user = testUser();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.updateUser(user.getId(), null, null, null);

        assertThat(user.getDisplayName()).isEqualTo("Test User");
        assertThat(user.getEmail()).isEqualTo("test@test.com");
        assertThat(user.getRole()).isEqualTo(Role.ROLE_MEMBER);
    }

    @Test
    void updateUser_notFound_shouldThrow() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(id, "Name", null, null))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void deactivateUser_shouldSetActiveToFalse() {
        User user = testUser();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.deactivateUser(user.getId());

        assertThat(user.getIsActive()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    void deactivateUser_notFound_shouldThrow() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deactivateUser(id))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void deleteUser_shouldCallRepository() {
        UUID id = UUID.randomUUID();
        when(userRepository.existsById(id)).thenReturn(true);

        userService.deleteUser(id);

        verify(userRepository).deleteById(id);
    }

    @Test
    void deleteUser_notFound_shouldThrow() {
        UUID id = UUID.randomUUID();
        when(userRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteUser(id))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
