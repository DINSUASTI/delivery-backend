package com.prueba.delivery.config;

import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

@Configuration
public class PostgresDataSourceConfig {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource dataSource(DataSourceProperties properties) {
        createDatabaseIfMissing(properties);
        return properties.initializeDataSourceBuilder().build();
    }

    private void createDatabaseIfMissing(DataSourceProperties properties) {
        String url = properties.getUrl();
        if (url == null || !url.contains("/delivery")) {
            return;
        }

        String adminUrl = url.replaceFirst("/delivery(\\?.*)?$", "/postgres$1");
        try (Connection connection = DriverManager.getConnection(adminUrl, properties.getUsername(), properties.getPassword());
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE DATABASE delivery");
        } catch (SQLException ignored) {
            // La base de datos ya existe o el usuario no tiene permisos; se continúa
        }
    }
}
