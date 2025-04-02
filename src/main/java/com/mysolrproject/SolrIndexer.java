package com.mysolrproject;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

public class SolrIndexer {
    private static final String SOLR_URL = "http://localhost:8983/solr/oracle-core";

    public static void main(String[] args) {
        System.out.println("Starting SolrIndexer...");
        try (SolrClient solrClient = new HttpSolrClient.Builder(SOLR_URL).build()) {
            System.out.println("SolrClient initialized with URL: " + SOLR_URL);
            try (Connection conn = OracleDBConnector.getConnection();
                 Statement stmt = conn.createStatement()) {
                System.out.println("Executing query: SELECT * FROM SIEBEL.S_EVT_ACT act WHERE act.CREATED >= TO_DATE('04/01/2025 00:00:00','MM/DD/YYYY HH24:MI:SS')");
                ResultSet rs = stmt.executeQuery("SELECT * FROM SIEBEL.S_EVT_ACT act WHERE act.CREATED >= TO_DATE('04/01/2025 00:00:00','MM/DD/YYYY HH24:MI:SS')");
                System.out.println("Query executed successfully.");

                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                while (rs.next()) {
                    SolrInputDocument document = new SolrInputDocument();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i);
                        String columnValue = rs.getString(i);
                        document.addField(columnName, columnValue);
                    }
                    System.out.println("Adding document to Solr: " + document);
                    solrClient.add(document);
                }
                solrClient.commit();
                System.out.println("Documents committed to Solr successfully.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("SolrIndexer finished.");
    }
}
