package com.mysolrproject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;
import java.io.InputStream;

public class OracleDBConnector {
    private static Properties loadProperties() throws Exception {
        Properties properties = new Properties();
        System.out.println("Loading properties from application.properties...");
        try (InputStream input = OracleDBConnector.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new Exception("Sorry, unable to find application.properties");
            }
            properties.load(input);
        }
        System.out.println("Properties loaded successfully.");
        return properties;
    }

    public static Connection getConnection() throws Exception {
        Properties properties = loadProperties();
        String url = properties.getProperty("oracle.db.url");
        String user = properties.getProperty("oracle.db.username");
        String password = properties.getProperty("oracle.db.password");

        // Print connection details to console
        System.out.println("Connecting to Oracle DB with URL: " + url);
        System.out.println("Username: " + user);

        Class.forName("oracle.jdbc.driver.OracleDriver");
        System.out.println("Oracle JDBC Driver loaded successfully.");
        Connection connection = DriverManager.getConnection(url, user, password);
        System.out.println("Connection established successfully.");
        return connection;
    }

    public static void main(String[] args) {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            System.out.println("Executing query: SELECT * FROM SIEBEL.S_EVT_ACT act WHERE act.CREATED >= TO_DATE('04/01/2025 00:00:00','MM/DD/YYYY HH24:MI:SS')");
            ResultSet rs = stmt.executeQuery("SELECT * FROM SIEBEL.S_EVT_ACT act WHERE act.CREATED >= TO_DATE('04/01/2025 00:00:00','MM/DD/YYYY HH24:MI:SS')");
            System.out.println("Query executed successfully.");
            while (rs.next()) {
                System.out.println("Data: " + rs.getString("ROW_ID"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
