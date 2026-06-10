package com.mysolrproject.dto;

import java.util.List;
import java.util.Map;

/**
 * Response DTO for Solr search. Documents use single-value fields (e.g. "ACCOUNTNAME": "Equinix").
 */
public record SearchResult(
        long numFound,
        List<Map<String, Object>> documents
) {}
