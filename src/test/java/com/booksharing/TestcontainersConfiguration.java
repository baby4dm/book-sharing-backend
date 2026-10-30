package com.booksharing;

import java.util.TimeZone;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Kyiv"));
    }

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
                .withDatabaseName("bookshare-db")
                .withUsername("root")
                .withPassword("root")
                .withCopyFileToContainer(
                        MountableFile.forHostPath("db/full_dump.sql"),
                        "/docker-entrypoint-initdb.d/01-schema.sql");
    }

}