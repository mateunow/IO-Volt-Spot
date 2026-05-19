package pl.voltspot.backend.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/test");
    }

    private void assertCommonFields(ResponseEntity<ApiError> response, HttpStatus expectedStatus, String expectedMessage) {
        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
        ApiError body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(expectedStatus.value());
        assertThat(body.error()).isEqualTo(expectedStatus.getReasonPhrase());
        assertThat(body.path()).isEqualTo("/api/test");
        assertThat(body.timestamp()).isNotNull();
        if (expectedMessage != null) {
            assertThat(body.message()).contains(expectedMessage);
        }
    }

    @Test
    void handleNotFound_returns404() {
        ResponseEntity<ApiError> response = handler.handleNotFound(
                new NotFoundException("missing"), request);

        assertCommonFields(response, HttpStatus.NOT_FOUND, "missing");
    }

    @Test
    void handleBadRequest_returns400() {
        ResponseEntity<ApiError> response = handler.handleBadRequest(
                new BadRequestException("bad input"), request);

        assertCommonFields(response, HttpStatus.BAD_REQUEST, "bad input");
    }

    @Test
    void handleUnauthorized_returns401() {
        ResponseEntity<ApiError> response = handler.handleUnauthorized(
                new UnauthorizedException("no auth"), request);

        assertCommonFields(response, HttpStatus.UNAUTHORIZED, "no auth");
    }

    @Test
    void handleForbidden_returns403() {
        ResponseEntity<ApiError> response = handler.handleForbidden(
                new ForbiddenException("denied"), request);

        assertCommonFields(response, HttpStatus.FORBIDDEN, "denied");
    }

    @Test
    void handleGeocoding_returns503() {
        ResponseEntity<ApiError> response = handler.handleGeocoding(
                new GeocodingException("geo error"), request);

        assertCommonFields(response, HttpStatus.SERVICE_UNAVAILABLE, "geo error");
    }

    @Test
    void handleValidation_combinesFieldErrors() throws NoSuchMethodException {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "object");
        bindingResult.addError(new FieldError("object", "email", "must be valid"));
        bindingResult.addError(new FieldError("object", "password", "must not be blank"));

        Method method = String.class.getMethod("length");
        MethodParameter parameter = new MethodParameter(method, -1);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ApiError> response = handler.handleValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ApiError body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.message()).contains("email: must be valid");
        assertThat(body.message()).contains("password: must not be blank");
        assertThat(body.message()).contains(";");
    }

    @Test
    void handleConstraint_returns400() {
        ConstraintViolationException ex = new ConstraintViolationException("constraint failed", Set.of());

        ResponseEntity<ApiError> response = handler.handleConstraint(ex, request);

        assertCommonFields(response, HttpStatus.BAD_REQUEST, "constraint failed");
    }

    @Test
    void handleOther_returns500() {
        ResponseEntity<ApiError> response = handler.handleOther(
                new RuntimeException("boom"), request);

        assertCommonFields(response, HttpStatus.INTERNAL_SERVER_ERROR, "boom");
    }
}
