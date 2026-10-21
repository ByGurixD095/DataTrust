package com.datatrust.infrastructure.validation;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import com.datatrust.application.validation.RowReader;
import com.datatrust.application.validation.TabularRow;

/** Lee un CSV en streaming y lo entrega como {@link TabularRow}. Adaptador de infraestructura. */
public final class CsvRowReader implements RowReader {

    private static final CSVFormat FORMAT = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreEmptyLines(true)
            .get();

    private final CSVParser parser;
    /** Columna normalizada -> posición en la cabecera del CSV. */
    private final Map<String, Integer> columnIndex = new HashMap<>();

    public CsvRowReader(InputStream content) throws IOException {
        this.parser = CSVParser.parse(new InputStreamReader(content, StandardCharsets.UTF_8), FORMAT);
        parser.getHeaderMap().forEach(
                (name, index) -> columnIndex.put(TabularRow.normalize(name), index));
    }

    @Override
    public Set<String> headers() {
        return Set.copyOf(columnIndex.keySet());
    }

    @Override
    public void forEachRow(RowConsumer consumer) {
        for (CSVRecord csvRecord : parser) {
            consumer.accept(toRow(csvRecord));
        }
    }

    @Override
    public void close() throws IOException {
        parser.close();
    }

    private TabularRow toRow(CSVRecord csvRecord) {
        Map<String, String> values = new HashMap<>();
        columnIndex.forEach((name, index) ->
                values.put(name, index < csvRecord.size() ? csvRecord.get(index) : ""));
        return new TabularRow(csvRecord.getRecordNumber(), values);
    }
}
