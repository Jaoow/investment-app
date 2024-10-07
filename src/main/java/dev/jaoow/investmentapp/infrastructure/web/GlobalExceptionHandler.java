package dev.jaoow.investmentapp.infrastructure.web;

import dev.jaoow.investmentapp.application.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PortfolioNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePortfolioNotFoundException(PortfolioNotFoundException ex) {
        ErrorResponse errorResponse = new ErrorResponse("Portfolio Not Found", ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(AssetMovementNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAssetMovementNotFoundException(AssetMovementNotFoundException ex) {
        ErrorResponse errorResponse = new ErrorResponse("Asset Movement Not Found", ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(PortfolioMismatchException.class)
    public ResponseEntity<ErrorResponse> handlePortfolioMismatchException(PortfolioMismatchException ex) {
        ErrorResponse errorResponse = new ErrorResponse("Portfolio Mismatch", ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AssetMovementImportException.class)
    public ResponseEntity<ErrorResponse> handleAssetMovementImportException(AssetMovementImportException ex) {
        ErrorResponse errorResponse = new ErrorResponse("Import Error", ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(TickerNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTickerNotFoundException(TickerNotFoundException ex) {
        ErrorResponse errorResponse = new ErrorResponse("Ticker Not Found", ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidTickerException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTickerException(InvalidTickerException ex) {
        ErrorResponse errorResponse = new ErrorResponse("Invalid Ticker", ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        ErrorResponse errorResponse = new ErrorResponse("Internal Server Error", "An unexpected error occurred.");
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
