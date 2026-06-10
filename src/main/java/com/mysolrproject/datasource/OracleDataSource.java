package com.mysolrproject.datasource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Provides Oracle (Siebel) database connections.
 * Connection properties are injected from application properties / environment variables.
 */
@Component
public class OracleDataSource {

    private static final Logger log = LoggerFactory.getLogger(OracleDataSource.class);

    private final String url;
    private final String username;
    private final String password;

    public OracleDataSource(
            @Value("${oracle.db.url}") String url,
            @Value("${oracle.db.username}") String username,
            @Value("${oracle.db.password}") String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public Connection getConnection() throws Exception {
        log.info("[ORACLE] Connecting - url={}, user={}", url, username);
        Class.forName("oracle.jdbc.driver.OracleDriver");
        Connection connection = DriverManager.getConnection(url, username, password);
        log.info("[ORACLE] Connection established successfully");
        return connection;
    }
}
