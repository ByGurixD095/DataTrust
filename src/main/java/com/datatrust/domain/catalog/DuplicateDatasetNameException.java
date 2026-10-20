package com.datatrust.domain.catalog;

public class DuplicateDatasetNameException extends RuntimeException{
    
    private final String datasetName;

    public DuplicateDatasetNameException(String datasetName) {
        super("Ya existe un dataset con el nombre '" + datasetName + "'");
        this.datasetName = datasetName;
    }

    public String datasetName() { return datasetName; }
}
