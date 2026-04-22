package pl.voltspot.backend.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class GeocodingException extends RuntimeException {
    public GeocodingException(String message, Throwable cause) {
        super(message, cause);
    }
    public GeocodingException(String message) {
        super(message);
    }
}