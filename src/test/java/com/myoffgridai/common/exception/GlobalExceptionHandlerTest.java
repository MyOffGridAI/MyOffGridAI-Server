package com.myoffgridai.common.exception;

import com.myoffgridai.common.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.core.MethodParameter;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleValidation_shouldReturn400() throws NoSuchMethodException {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult("target", "target");
        bindingResult.addError(new FieldError("target", "username", "must not be blank"));

        MethodParameter param = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("handleValidation_shouldReturn400"), -1);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(param, bindingResult);

        ResponseEntity<ApiResponse<Object>> response = handler.handleValidation(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).contains("username");
    }

    @Test
    void handleUsernameNotFound_shouldReturn404() {
        ResponseEntity<ApiResponse<Object>> response =
                handler.handleUsernameNotFound(new UsernameNotFoundException("User not found"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().isSuccess()).isFalse();
    }

    @Test
    void handleBadCredentials_shouldReturn401() {
        ResponseEntity<ApiResponse<Object>> response =
                handler.handleBadCredentials(new BadCredentialsException("Bad"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid username or password");
    }

    @Test
    void handleAccessDenied_shouldReturn403() {
        ResponseEntity<ApiResponse<Object>> response =
                handler.handleAccessDenied(new AccessDeniedException("Denied"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void handleEntityNotFound_shouldReturn404() {
        ResponseEntity<ApiResponse<Object>> response =
                handler.handleEntityNotFound(new EntityNotFoundException("Not found"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void handleDuplicate_shouldReturn409() {
        ResponseEntity<ApiResponse<Object>> response =
                handler.handleDuplicate(new DuplicateResourceException("Duplicate"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void handleIllegalArgument_shouldReturn400() {
        ResponseEntity<ApiResponse<Object>> response =
                handler.handleIllegalArgument(new IllegalArgumentException("Bad arg"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleFortressActive_shouldReturn403() {
        ResponseEntity<ApiResponse<Object>> response =
                handler.handleFortressActive(new FortressActiveException("Locked"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void handleGeneral_shouldReturn500() {
        ResponseEntity<ApiResponse<Object>> response =
                handler.handleGeneral(new RuntimeException("Unexpected"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
    }
}
