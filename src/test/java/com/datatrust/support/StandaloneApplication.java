package com.datatrust.support;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.datatrust.DatatrustApplication;

public final class StandaloneApplication {

    public static ConfigurableApplicationContext start(PostgreSQLContainer database) {
        return new SpringApplicationBuilder(DatatrustApplication.class)
                .web(WebApplicationType.NONE)
                .properties(
                        "spring.datasource.url=" + database.getJdbcUrl(),
                        "spring.datasource.username=" + database.getUsername(),
                        "spring.datasource.password=" + database.getPassword())
                .run();
    }

    private StandaloneApplication() {
    }
}