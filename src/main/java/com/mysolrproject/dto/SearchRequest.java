package com.mysolrproject.dto;

import java.util.List;

/**
 * Request body for POST /api/search. All search parameters are sent in the body.
 */
public record SearchRequest(
        /** Search query (e.g. "Equinix", "*:*"). Default "*:*" if null/blank. */
        String q,
        /** Max number of rows to return (default 10, max 1000). */
        Integer rows,
        /** Optional list of field names to search in (qf). If empty/null, default searchable fields are used. */
        List<String> fields
) {
    public String effectiveQuery() {
        return (q != null && !q.isBlank()) ? q : "*:*";
    }

    public int effectiveRows() {
        if (rows == null || rows <= 0) return 10;
        return Math.min(rows, 1000);
    }

    /** If non-empty, use as qf; otherwise caller uses default fields. */
    public List<String> effectiveFields() {
        return (fields != null && !fields.isEmpty()) ? fields : null;
    }
}
