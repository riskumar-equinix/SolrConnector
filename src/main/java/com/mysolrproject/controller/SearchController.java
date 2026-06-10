package com.mysolrproject.controller;

import com.mysolrproject.dto.SearchRequest;
import com.mysolrproject.dto.SearchResult;
import com.mysolrproject.service.SolrSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private static final Logger log = LoggerFactory.getLogger(SearchController.class);

    private final SolrSearchService searchService;

    public SearchController(SolrSearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * GET /api/search?q=...&rows=... — search Solr via query params.
     */
    @GetMapping
    public ResponseEntity<SearchResult> searchGet(
            @RequestParam(defaultValue = "*:*") String q,
            @RequestParam(defaultValue = "10") int rows) {
        log.info("[API] GET /api/search - q='{}', rows={}", q, rows);
        SearchResult result = searchService.search(new SearchRequest(q, rows, null));
        log.info("[API] GET /api/search - numFound={}", result.numFound());
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/search — search Solr via JSON body.
     */
    @PostMapping
    public ResponseEntity<SearchResult> searchPost(@RequestBody SearchRequest request) {
        SearchRequest req = request != null ? request : new SearchRequest(null, null, null);
        log.info("[API] POST /api/search - q='{}', rows={}, fields={}",
                req.effectiveQuery(), req.effectiveRows(), req.fields());
        SearchResult result = searchService.search(req);
        log.info("[API] POST /api/search - numFound={}", result.numFound());
        return ResponseEntity.ok(result);
    }
}
