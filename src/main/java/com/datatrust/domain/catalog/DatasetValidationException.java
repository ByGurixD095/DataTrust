package com.datatrust.domain.catalog;

import com.datatrust.domain.enums.DatasetErrorCode;

public class DatasetValidationException extends RuntimeException {

    private final DatasetErrorCode code;
    private final String field;

    public DatasetValidationException(DatasetErrorCode code, String field, String message) {
        super(message);
        this.code = code;
        this.field = field;
    }

    public DatasetErrorCode code() { return code; }
    public String field() { return field; }
}