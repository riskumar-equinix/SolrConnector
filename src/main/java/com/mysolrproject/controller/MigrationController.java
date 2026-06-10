package com.mysolrproject.controller;

import com.mysolrproject.dto.MigrationResult;
import com.mysolrproject.service.SolrIndexerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/migrate")
public class MigrationController {

    private static final Logger log = LoggerFactory.getLogger(MigrationController.class);

    private final SolrIndexerService indexerService;

    public MigrationController(SolrIndexerService indexerService) {
        this.indexerService = indexerService;
    }

    /**
     * POST /api/migrate/siebel — triggers full data migration from Siebel (Oracle) to Solr.
     */
    @PostMapping("/siebel")
    public ResponseEntity<MigrationResult> migrateSiebel() {
        log.info("[API] POST /api/migrate/siebel - starting Siebel migration");
        MigrationResult result = indexerService.runMigration();
        log.info("[API] POST /api/migrate/siebel - done: indexed={}, skipped={}, elapsedMs={}",
                result.documentsIndexed(), result.documentsSkipped(), result.elapsedMs());
        return ResponseEntity.ok(result);
    }
}
