package com.datatrust.datatrust.support;

import org.testcontainers.postgresql.PostgreSQLContainer;

public final class SharedPostgres {

    public static final PostgreSQLContainer CONTAINER =
            new PostgreSQLContainer("postgres:16-alpine");

    static {
        CONTAINER.start();
    }

    private SharedPostgres() {
    }
}