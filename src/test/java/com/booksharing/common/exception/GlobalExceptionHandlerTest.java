package com.booksharing.common.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Method;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@DisplayName("Обробка помилок API")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @SuppressWarnings("unused")
    static class Sample {
        void byId(@PathVariable UUID id, @RequestParam Integer page) {
        }
    }

    private static MethodParameter parameter(int index) throws NoSuchMethodException {
        Method method = Sample.class.getDeclaredMethod("byId", UUID.class, Integer.class);
        return new MethodParameter(method, index);
    }

    @Test
    @DisplayName("бізнес-помилки отримують свої коди й текст")
    void businessErrors() {
        assertThat(handler.handleNotFound(new ResourceNotFoundException("Немає")).status()).isEqualTo(404);
        assertThat(handler.handleIllegalArgument(new IllegalArgumentException("Погано")).message()).isEqualTo("Погано");
        assertThat(handler.handleIllegalState(new IllegalStateException("Конфлікт")).status()).isEqualTo(409);
        assertThat(handler.handleAccountRestricted(new AccountRestrictedException("Обмежено")).status()).isEqualTo(403);
        assertThat(handler.handleAccessDenied(new AccessDeniedException("x")).message()).isEqualTo("Немає доступу до цієї дії");
        assertThat(handler.handleInvalidCredentials(new InvalidCredentialsException("Невірно")).status()).isEqualTo(401);
    }

    @Test
    @DisplayName("помилки запиту Spring MVC")
    void mvcErrors() {
        assertThat(handler.handleNoResource(mock(NoResourceFoundException.class)).status()).isEqualTo(404);
        assertThat(handler.handleNotReadable(
                new HttpMessageNotReadableException("bad", new MockHttpInputMessage(new byte[0]))).status()).isEqualTo(400);
        assertThat(handler.handleMissingParam(new MissingServletRequestParameterException("q", "String")).message())
                .isEqualTo("Не вказано параметр «q»");
        assertThat(handler.handleMethodNotSupported(new HttpRequestMethodNotSupportedException("PUT")).message())
                .contains("PUT");
        assertThat(handler.handleDataIntegrity(
                new DataIntegrityViolationException("dup", new RuntimeException("duplicate key"))).status()).isEqualTo(409);
        assertThat(handler.handleMaxUpload(new MaxUploadSizeExceededException(10)).status()).isEqualTo(413);
        assertThat(handler.handleMediaType(new HttpMediaTypeNotSupportedException("text/plain")).status()).isEqualTo(415);
        assertThat(handler.handleGeneric(new RuntimeException("boom")).status()).isEqualTo(500);
    }

    @Test
    @DisplayName("некоректний id у шляху дає 404, а некоректний параметр 400")
    void typeMismatch() throws NoSuchMethodException {
        ResponseEntity<ErrorResponse> path = handler.handleTypeMismatch(new MethodArgumentTypeMismatchException(
                "abc", UUID.class, "id", parameter(0), new IllegalArgumentException()));
        ResponseEntity<ErrorResponse> param = handler.handleTypeMismatch(new MethodArgumentTypeMismatchException(
                "abc", Integer.class, "page", parameter(1), new NumberFormatException()));

        assertThat(path.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(param.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(param.getBody().message()).contains("page");
    }

    @Test
    @DisplayName("помилки валідації збираються за полями")
    void validation() throws NoSuchMethodException {
        BeanPropertyBindingResult result = new BeanPropertyBindingResult(new Object(), "request");
        result.addError(new FieldError("request", "email", "некоректний email"));
        result.addError(new FieldError("request", "password", "закороткий пароль"));

        ValidationErrorResponse response = handler.handleValidation(new MethodArgumentNotValidException(parameter(0), result));

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.fieldErrors()).containsEntry("email", "некоректний email")
                .containsEntry("password", "закороткий пароль");
    }

    @Test
    @DisplayName("стандартні помилки Spring зберігають свій статус")
    void springErrors() {
        ResponseEntity<ErrorResponse> withDetail = handler.handleSpringError(new ErrorResponseException(
                HttpStatus.CONFLICT, ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Деталь"), null));
        ResponseEntity<ErrorResponse> serverError = handler.handleSpringError(new ErrorResponseException(HttpStatus.BAD_GATEWAY));

        assertThat(withDetail.getStatusCode().value()).isEqualTo(409);
        assertThat(withDetail.getBody().message()).isEqualTo("Деталь");
        assertThat(serverError.getStatusCode().value()).isEqualTo(502);
        assertThat(serverError.getBody().message()).isEqualTo("Помилка запиту");
    }

    @Test
    @DisplayName("фабрики відповідей")
    void factories() {
        assertThat(ErrorResponse.of("x", 400).timestamp()).isNotNull();
        assertThat(ValidationErrorResponse.of(java.util.Map.of()).message()).isEqualTo("Помилка валідації вхідних даних");
    }
}
