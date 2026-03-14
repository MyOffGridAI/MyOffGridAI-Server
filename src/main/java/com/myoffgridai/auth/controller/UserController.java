package com.myoffgridai.auth.controller;

import com.myoffgridai.auth.dto.UpdateUserRequest;
import com.myoffgridai.auth.dto.UserDetailDto;
import com.myoffgridai.auth.dto.UserSummaryDto;
import com.myoffgridai.auth.model.User;
import com.myoffgridai.auth.service.UserService;
import com.myoffgridai.common.response.ApiResponse;
import com.myoffgridai.config.AppConstants;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for user management operations.
 *
 * <p>Provides CRUD endpoints for user accounts with role-based
 * access control enforced via {@code @PreAuthorize}. All responses
 * are wrapped in {@link ApiResponse}.</p>
 */
@RestController
@RequestMapping(AppConstants.API_USERS)
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    /**
     * Constructs the controller with the user service.
     *
     * @param userService the user management service
     */
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Lists all users with pagination. Restricted to OWNER and ADMIN roles.
     *
     * @param page the page number (zero-based, default 0)
     * @param size the page size (default {@link AppConstants#DEFAULT_PAGE_SIZE})
     * @return paginated list of user summaries
     */
    @GetMapping
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserSummaryDto>>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + AppConstants.DEFAULT_PAGE_SIZE) int size) {
        log.info("Listing users - page: {}, size: {}", page, size);
        Page<UserSummaryDto> result = userService.listUsers(page, size);
        return ResponseEntity.ok(
                ApiResponse.paginated(result.getContent(), result.getTotalElements(), page, size));
    }

    /**
     * Retrieves a user's detailed profile by ID.
     *
     * <p>OWNER and ADMIN can view any user. MEMBER and VIEWER can only view
     * their own profile.</p>
     *
     * @param id        the user UUID
     * @param principal the authenticated user
     * @return the user detail
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDetailDto>> getUser(
            @PathVariable UUID id,
            @AuthenticationPrincipal User principal) {
        log.info("Get user request for ID: {}", id);

        boolean isAdminOrOwner = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(AppConstants.ROLE_OWNER)
                        || a.getAuthority().equals(AppConstants.ROLE_ADMIN));

        if (!isAdminOrOwner && !principal.getId().equals(id)) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("Access denied"));
        }

        UserDetailDto user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    /**
     * Updates a user's profile (displayName, email, role).
     * Restricted to OWNER and ADMIN roles.
     *
     * @param id      the user UUID
     * @param request the update request
     * @return the updated user detail
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserDetailDto>> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {
        log.info("Update user request for ID: {}", id);
        UserDetailDto updated = userService.updateUser(
                id, request.displayName(), request.email(), request.role());
        return ResponseEntity.ok(ApiResponse.success(updated, "User updated successfully"));
    }

    /**
     * Deactivates a user account. Restricted to OWNER role only.
     *
     * @param id the user UUID
     * @return confirmation message
     */
    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<Void>> deactivateUser(@PathVariable UUID id) {
        log.info("Deactivate user request for ID: {}", id);
        userService.deactivateUser(id);
        return ResponseEntity.ok(ApiResponse.success(null, "User deactivated"));
    }

    /**
     * Permanently deletes a user and all associated data. Restricted to OWNER role only.
     *
     * @param id the user UUID
     * @return confirmation message
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable UUID id) {
        log.info("Delete user request for ID: {}", id);
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success(null, "User deleted"));
    }
}
