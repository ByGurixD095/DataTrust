package com.datatrust.api.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import com.datatrust.support.SharedPostgres;

@SpringBootTest
@AutoConfigureMockMvc
class ValidationApiTest {

    private static final String DATASETS = "/api/v1/datasets";

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

    private String authorization;
    private String datasetId;

    @BeforeEach
    void setUp() throws Exception {
        jdbc.update("DELETE FROM datasets");
        jdbc.update("DELETE FROM users");
        MvcResult result = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "validator",
                                  "password": "validation-test-password-123",
                                  "displayName": "Validator",
                                  "email": "validator@example.com"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        authorization = "Bearer " + jsonValue(result.getResponse().getContentAsString(), "accessToken");
        datasetId = createDataset();
    }

    @Test
    void validCsvReturns200WithValidReport() throws Exception {
        mvc.perform(upload(datasetId, "id,email\n1,a@x.es\n2,b@x.es\n"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows").value(2))
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void invalidCellsAreCountedPerColumn() throws Exception {
        mvc.perform(upload(datasetId, "id,email\nabc,\n3,c@x.es\n"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.errorsByColumn.id").value(1))
                .andExpect(jsonPath("$.errorsByColumn.email").value(1));
    }

    @Test
    void sampleExplainsWhatIsWrong() throws Exception {
        mvc.perform(upload(datasetId, "id,email\nabc,a@x.es\n"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.samples[0].column").value("id"))
                .andExpect(jsonPath("$.samples[0].value").value("abc"))
                .andExpect(jsonPath("$.samples[0].reason").value(containsString("INTEGER")));
    }

    @Test
    void missingColumnIsReportedInHeaderErrors() throws Exception {
        mvc.perform(upload(datasetId, "id\n1\n"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.headerErrors[0]").value("Falta la columna 'email'"));
    }

    @Test
    void malformedCsvReturns400() throws Exception {
        mvc.perform(upload(datasetId, "id,email\n1,\"sin cerrar\n"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Contenido no válido"));
    }

    @Test
    void requestWithoutFileReturns400() throws Exception {
        mvc.perform(multipart(DATASETS + "/" + datasetId + "/validations")
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownDatasetReturns404() throws Exception {
        String unknown = UUID.randomUUID().toString();

        mvc.perform(upload(unknown, "id,email\n1,a@x.es\n"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.datasetId").value(unknown));
    }

    @Test
    void validationRequiresAuthentication() throws Exception {
        mvc.perform(multipart(DATASETS + "/" + datasetId + "/validations")
                        .file(csvFile("id,email\n1,a@x.es\n")))
                .andExpect(status().isUnauthorized());
    }

    private MockMultipartHttpServletRequestBuilder upload(String id, String csv) {
        return multipart(DATASETS + "/" + id + "/validations")
                .file(csvFile(csv))
                .header(HttpHeaders.AUTHORIZATION, authorization);
    }

    private static MockMultipartFile csvFile(String csv) {
        return new MockMultipartFile("file", "clientes.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));
    }

    private String createDataset() throws Exception {
        String body = """
                {
                  "name": "clientes",
                  "description": "Prueba de validación",
                  "format": "CSV",
                  "major": 1,
                  "minor": 0,
                  "patch": 0,
                  "columns": %s
                }
                """.formatted(ID_AND_EMAIL);
        MvcResult result = mvc.perform(post(DATASETS)
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        String location = result.getResponse().getHeader(HttpHeaders.LOCATION);
        return location.substring(location.lastIndexOf('/') + 1);
    }

    private static String jsonValue(String json, String field) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*\"([^\"]+)\"")
                .matcher(json);
        assertThat(matcher.find()).as("JSON field %s", field).isTrue();
        return matcher.group(1);
    }
}
