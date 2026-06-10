package com.mysolrproject.dto;

/**
 * Response DTO for the migration (Oracle → Solr) operation.
 */
public record MigrationResult(
        long documentsIndexed,
        long documentsSkipped,
        long elapsedMs
) {}
