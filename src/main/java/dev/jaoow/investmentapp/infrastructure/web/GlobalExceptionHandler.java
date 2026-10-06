package dev.jaoow.investmentapp.infrastructure.web;

import dev.jaoow.investmentapp.application.exception.*;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Resource not found.", ex.getMessage());
    }

    @ExceptionHandler(PortfolioNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePortfolioNotFoundException(PortfolioNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, "PORTFOLIO_NOT_FOUND", "Portfolio not found.", ex.getMessage());
    }

    @ExceptionHandler(AssetMovementNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAssetMovementNotFoundException(AssetMovementNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, "ASSET_MOVEMENT_NOT_FOUND", "Asset movement not found.", ex.getMessage());
    }

    @ExceptionHandler(PortfolioMismatchException.class)
    public ResponseEntity<ErrorResponse> handlePortfolioMismatchException(PortfolioMismatchException ex) {
        return error(HttpStatus.BAD_REQUEST, "PORTFOLIO_MISMATCH", "Portfolio mismatch.", ex.getMessage());
    }

    @ExceptionHandler(AssetMovementImportException.class)
    public ResponseEntity<ErrorResponse> handleAssetMovementImportException(AssetMovementImportException ex) {
        return error(HttpStatus.BAD_REQUEST, "ASSET_MOVEMENT_IMPORT_FAILED", "Asset movement import failed.", ex.getMessage());
    }

    @ExceptionHandler(TickerNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTickerNotFoundException(TickerNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, "TICKER_NOT_FOUND", "Ticker not found.", ex.getMessage());
    }

    @ExceptionHandler(InvalidTickerException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTickerException(InvalidTickerException ex) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_TICKER", "Ticker is invalid.", ex.getMessage());
    }

    @ExceptionHandler(InvalidAllocationException.class)
    public ResponseEntity<ErrorResponse> handleInvalidAllocationException(InvalidAllocationException ex) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_ALLOCATION", "Allocation is invalid.", ex.getMessage());
    }

    @ExceptionHandler(InvalidPortfolioPositionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPortfolioPositionException(InvalidPortfolioPositionException ex) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_PORTFOLIO_POSITION", "Portfolio position is invalid.", ex.getMessage());
    }

    @ExceptionHandler(InvalidPortfolioShareException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPortfolioShareException(InvalidPortfolioShareException ex) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_PORTFOLIO_SHARE", "Portfolio share is invalid.", ex.getMessage());
    }

    @ExceptionHandler(MarketDataUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleMarketDataUnavailableException(MarketDataUnavailableException ex) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, "MARKET_DATA_UNAVAILABLE", "Market data is unavailable.", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();

        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed.", errors);
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAuthorizationDeniedException(AuthorizationDeniedException ex) {
        return error(HttpStatus.FORBIDDEN, "FORBIDDEN", "Access denied.", ex.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFoundException(NoResourceFoundException ex) {
        return error(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Resource not found.", Map.of("path", ex.getResourcePath()));
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ErrorResponse> handleJwtException(JwtException ex) {
        String code = ex instanceof io.jsonwebtoken.ExpiredJwtException ? "TOKEN_EXPIRED" : "INVALID_TOKEN";
        String detail = ex instanceof io.jsonwebtoken.ExpiredJwtException ? "Token has expired." : "Invalid token.";
        return error(HttpStatus.UNAUTHORIZED, code, "Authentication failed.", detail);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("An unexpected error occurred", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "An unexpected error occurred.", null);
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String code, String message, Object detail) {
        Map<String, Object> details = new LinkedHashMap<>();
        if (detail instanceof Map<?, ?> values) {
            values.forEach((key, value) -> details.put(String.valueOf(key), value));
        } else if (detail != null) {
            details.put("reason", detail);
        }
        return ResponseEntity.status(status).body(new ErrorResponse(Instant.now(), status.value(), code, message, details));
    }
}
