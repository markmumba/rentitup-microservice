package com.rentitup.catalog_service.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;


@Configuration
public class CacheDbConfig {

    @Bean
    @Primary
    public DataSource dataSource(
            @org.springframework.beans.factory.annotation.Value("${spring.datasource.url}") String url,
            @org.springframework.beans.factory.annotation.Value("${spring.datasource.username}") String username,
            @org.springframework.beans.factory.annotation.Value("${spring.datasource.password}") String password) {
        return DataSourceBuilder.create()
                .url(url)
                .username(username)
                .password(password)
                .build();
    }

    @Bean
    @ConfigurationProperties("cache-db")
    public DataSource cacheDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    public JdbcTemplate cacheJdbcTemplate(@Qualifier("cacheDataSource") DataSource ds) {
        return new JdbcTemplate(ds);
    }

}
