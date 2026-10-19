package com.datatrust.datatrust.api.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.datatrust.datatrust.application.catalog.DatasetNotFoundException;
import com.datatrust.datatrust.domain.catalog.DatasetValidationException;
import com.datatrust.datatrust.domain.catalog.DuplicateDatasetNameException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DatasetNotFoundException.class)
    ProblemDetail handleNotFound(DatasetNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DuplicateDatasetNameException.class)
    ProblemDetail handleDuplicateName(DuplicateDatasetNameException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(DatasetValidationException.class)
    ProblemDetail handleValidation(DatasetValidationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setProperty("code", ex.code());
        problem.setProperty("field", ex.field());
        return problem;
    }

}