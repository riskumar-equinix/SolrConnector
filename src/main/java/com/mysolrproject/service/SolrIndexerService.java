package com.mysolrproject.service;

import com.mysolrproject.datasource.OracleDataSource;
import com.mysolrproject.dto.MigrationResult;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;

import static com.mysolrproject.constants.Constants.FETCH_PAGE_SIZE;
import static com.mysolrproject.constants.Constants.SEARCHABLE_TASK_QUERY;

@Service
public class SolrIndexerService {

    private static final Logger log = LoggerFactory.getLogger(SolrIndexerService.class);
    private static final int BATCH_SIZE = 500;
    private static final String PAGINATED_QUERY =
            "SELECT * FROM (" + SEARCHABLE_TASK_QUERY + ") OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

    private final SolrClient solrClient;
    private final OracleDataSource oracleDataSource;

    public SolrIndexerService(SolrClient solrClient, OracleDataSource oracleDataSource) {
        this.solrClient = solrClient;
        this.oracleDataSource = oracleDataSource;
    }

    /**
     * Runs the full migration: reads from Oracle (paginated), indexes into Solr in batches, then commits.
     */
    public MigrationResult runMigration() {
        long startMs = System.currentTimeMillis();
        int totalAdded = 0;
        int totalSkipped = 0;
        int pageNum = 0;
        int offset = 0;

            log.info("[MIGRATION] Step 1: Starting migration (batchSize={}, fetchPageSize={})", BATCH_SIZE, FETCH_PAGE_SIZE);

        try (Connection conn = oracleDataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(PAGINATED_QUERY)) {
            stmt.setFetchSize(FETCH_PAGE_SIZE);
            log.info("[MIGRATION] Step 2: Oracle connection obtained, prepared statement ready (paginated query with OFFSET/FETCH)");

            while (true) {
                stmt.setInt(1, offset);
                stmt.setInt(2, FETCH_PAGE_SIZE);
                try (ResultSet rs = stmt.executeQuery()) {
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    if (pageNum == 0) {
                        List<String> columns = new ArrayList<>(columnCount);
                        for (int i = 1; i <= columnCount; i++) {
                            columns.add(metaData.getColumnName(i));
                        }
                        log.info("[MIGRATION] Step 3: Query columns (count={}): {}", columnCount, columns);
                    }
                    List<SolrInputDocument> batch = new ArrayList<>(BATCH_SIZE);
                    int addedInPage = 0;
                    int skippedInPage = 0;
                    int rowsReadInPage = 0;

                    while (rs.next()) {
                        rowsReadInPage++;
                        SolrInputDocument document = new SolrInputDocument();
                        boolean skipDocument = false;

                        for (int i = 1; i <= columnCount; i++) {
                            String columnName = metaData.getColumnName(i);
                            String columnValue = rs.getString(i);

                            if ("CSN".equals(columnName) && columnValue != null) {
                                try {
                                    Long.parseLong(columnValue);
                                    document.addField(columnName, columnValue);
                                } catch (NumberFormatException e) {
                                    log.warn("[MIGRATION] Data validation: skipping document with non-numeric CSN: {}", columnValue);
                                    skipDocument = true;
                                    break;
                                }
                            } else {
                                document.addField(columnName, columnValue);
                            }
                        }

                        if (!skipDocument) {
                            batch.add(document);
                            if (batch.size() >= BATCH_SIZE) {
                                addedInPage += sendBatch(batch);
                                batch.clear();
                            }
                        } else {
                            skippedInPage++;
                        }
                    }

                    if (!batch.isEmpty()) {
                        addedInPage += sendBatch(batch);
                    }

                    totalAdded += addedInPage;
                    totalSkipped += skippedInPage;
                    pageNum++;

                    int fromRow = offset + 1;
                    int toRow = offset + rowsReadInPage;
                    log.info("[MIGRATION] Step 4 (page {}): data range rows {}-{} | indexedInPage={} | skippedInPage={} | totalIndexedSoFar={}",
                            pageNum, fromRow, toRow, addedInPage, skippedInPage, totalAdded);

                    if (rowsReadInPage < FETCH_PAGE_SIZE) {
                        log.info("[MIGRATION] Step 5: Last page reached (rowsRead={} < {}), stopping pagination", rowsReadInPage, FETCH_PAGE_SIZE);
                        break;
                    }
                    offset += FETCH_PAGE_SIZE;
                }
            }

            log.info("[MIGRATION] Step 6: Committing all documents to Solr");
            solrClient.commit();
            long elapsedMs = System.currentTimeMillis() - startMs;
            log.info("[MIGRATION] Step 7: Migration complete - totalIndexed={}, totalSkipped={}, totalPages={}, elapsedMs={} ({} sec)",
                    totalAdded, totalSkipped, pageNum, elapsedMs, elapsedMs / 1000);
            return new MigrationResult(totalAdded, totalSkipped, elapsedMs);
        } catch (Exception e) {
            log.error("[MIGRATION] Migration failed", e);
            throw new RuntimeException("Migration failed: " + e.getMessage(), e);
        }
    }

    private int sendBatch(List<SolrInputDocument> batch) {
        try {
            solrClient.add(batch);
            if (!batch.isEmpty()) {
                Object firstRowId = batch.get(0).getFieldValue("ROW_ID");
                Object firstActivity = batch.get(0).getFieldValue("ACTIVITYNUMBER");
                log.debug("[MIGRATION] Batch sent to Solr: size={}, sample ROW_ID={}, ACTIVITYNUMBER={}", batch.size(), firstRowId, firstActivity);
            }
            return batch.size();
        } catch (Exception batchEx) {
            log.warn("[MIGRATION] Batch add failed, falling back to single-doc add: {}", batchEx.getMessage());
            int added = 0;
            for (SolrInputDocument doc : batch) {
                try {
                    solrClient.add(doc);
                    added++;
                } catch (Exception e) {
                    Object rowId = doc.getFieldValue("ROW_ID");
                    log.warn("[MIGRATION] Solr rejected record, skipping ROW_ID={}: {}", rowId != null ? rowId : "?", e.getMessage());
                }
            }
            return added;
        }
    }
}
