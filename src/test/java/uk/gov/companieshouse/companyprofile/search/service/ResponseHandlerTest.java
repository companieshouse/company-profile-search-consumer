package uk.gov.companieshouse.companyprofile.search.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import consumer.exception.NonRetryableErrorException;
import consumer.exception.RetryableErrorException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;

@ExtendWith(MockitoExtension.class)
class ResponseHandlerTest {

    private final ResponseHandler responseHandler = new ResponseHandler();

    @Mock
    private ApiErrorResponseException apiErrorResponseException;

    @ParameterizedTest
    @MethodSource("scenarios")
    void shouldHandleApiErrorResponseScenarios(HttpStatus apiResponseStatus,
            Class<RuntimeException> expectedException) {
        // given
        when(apiErrorResponseException.getStatusCode()).thenReturn(apiResponseStatus.value());

        // when
        Executable executable = () -> responseHandler.handle(apiErrorResponseException);

        // then
        assertThrows(expectedException, executable);
    }

    @Test
    void shouldHandleURIValidationException() {
        // given
        URIValidationException exception = new URIValidationException("Invalid URI");

        // when
        Executable executable = () -> responseHandler.handle(exception);

        // then
        assertThrows(NonRetryableErrorException.class, executable);
    }

    private static Stream<Arguments> scenarios() {
        return Stream.of(
                Arguments.of(HttpStatus.BAD_REQUEST, NonRetryableErrorException.class),
                Arguments.of(HttpStatus.CONFLICT, NonRetryableErrorException.class),
                Arguments.of(HttpStatus.UNAUTHORIZED, RetryableErrorException.class),
                Arguments.of(HttpStatus.FORBIDDEN, RetryableErrorException.class),
                Arguments.of(HttpStatus.NOT_FOUND, RetryableErrorException.class),
                Arguments.of(HttpStatus.METHOD_NOT_ALLOWED, RetryableErrorException.class),
                Arguments.of(HttpStatus.INTERNAL_SERVER_ERROR, RetryableErrorException.class),
                Arguments.of(HttpStatus.SERVICE_UNAVAILABLE, RetryableErrorException.class)
        );
    }
}