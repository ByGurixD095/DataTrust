package com.datatrust.api.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.datatrust.support.SharedPostgres;

@SpringBootTest
@AutoConfigureMockMvc
class DatasetApiTest {

    private static final String BASE = "/api/v1/datasets";

    private static final String ID_AND_EMAIL = """
            [
              {"name": "id", "position": 1, "expectedType": "INTEGER", "required": true},
              {"name": "email", "position": 2, "expectedType": "STRING", "required": true}
            ]
            """;

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", SharedPostgres.CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", SharedPostgres.CONTAINER::getUsername);
        registry.add("spring.datasource.password", SharedPostgres.CONTAINER::getPassword);
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() {
        // Las columnas se borran en cascada por la clave foránea
        jdbc.update("DELETE FROM datasets");
    }

    @Test
    void createReturns201WithLocation() throws Exception {
        MvcResult result = mvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(datasetJson("clientes", ID_AND_EMAIL)))
                .andExpect(status().isCreated())
                .andReturn();

        assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION))
                .matches("http://localhost/api/v1/datasets/[0-9a-f\\-]{36}");
    }

    @Test
    void getReturnsColumnsOrderedByPosition() throws Exception {
        String columnsReversed = """
                [
                  {"name": "email", "position": 2, "expectedType": "STRING", "required": true},
                  {"name": "id", "position": 1, "expectedType": "INTEGER", "required": true}
                ]
                """;
        String id = createAndGetId(datasetJson("clientes", columnsReversed));

        mvc.perform(get(BASE + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("clientes"))
                .andExpect(jsonPath("$.columns[0].name").value("id"))
                .andExpect(jsonPath("$.columns[1].name").value("email"));
    }

    @Test
    void duplicateNameIgnoringCaseReturns409() throws Exception {
        createAndGetId(datasetJson("clientes", ID_AND_EMAIL));

        mvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(datasetJson("CLIENTES", ID_AND_EMAIL)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.conflictingName").value("CLIENTES"));
    }

    @Test
    void missingColumnsReturns400WithFieldErrors() throws Exception {
        mvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(datasetJson("clientes", "[]")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.columns").exists());
    }

    @Test
    void duplicateColumnNamesReturn400WithCode() throws Exception {
        String duplicated = """
                [
                  {"name": "id", "position": 1, "expectedType": "INTEGER", "required": true},
                  {"name": "ID", "position": 2, "expectedType": "INTEGER", "required": true}
                ]
                """;

        mvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(datasetJson("clientes", duplicated)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COLUMN_NAME_DUPLICATED"))
                .andExpect(jsonPath("$.field").value("columns.name"));
    }

    @Test
    void unknownIdReturns404() throws Exception {
        String id = UUID.randomUUID().toString();

        mvc.perform(get(BASE + "/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.datasetId").value(id));
    }

    @Test
    void listRespectsPageAndSizeLimit() throws Exception {
        createAndGetId(datasetJson("uno", ID_AND_EMAIL));
        createAndGetId(datasetJson("dos", ID_AND_EMAIL));
        createAndGetId(datasetJson("tres", ID_AND_EMAIL));

        mvc.perform(get(BASE).param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mvc.perform(get(BASE).param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mvc.perform(get(BASE).param("size", "500"))
                .andExpect(status().isBadRequest());
    }

    private String createAndGetId(String json) throws Exception {
        MvcResult result = mvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn();

        String location = result.getResponse().getHeader(HttpHeaders.LOCATION);
        return location.substring(location.lastIndexOf('/') + 1);
    }

    private static String datasetJson(String name, String columns) {
        return """
                {
                  "name": "%s",
                  "description": "Prueba de integración",
                  "format": "CSV",
                  "major": 1,
                  "minor": 2,
                  "patch": 3,
                  "columns": %s
                }
                """.formatted(name, columns);
    }
}