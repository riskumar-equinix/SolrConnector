package com.mysolrproject.service;

import com.mysolrproject.dto.SearchResult;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SolrSearchService {

    private static final Logger log = LoggerFactory.getLogger(SolrSearchService.class);
    private static final String QUERY_FIELDS =
            "ACTIVITYNUMBER ORDERNUMBER SALESORDERNUMBER ACCOUNTNAME CSN IBXNAME ACTSYSTEMNAME ORDERSYSTEMNAME SERIALNUMBER ASSETNUMBER";

    private final SolrClient solrClient;

    public SolrSearchService(SolrClient solrClient) {
        this.solrClient = solrClient;
    }

    /**
     * Searches Solr using edismax and returns documents as maps with single-value fields.
     * If request.fields is provided, those are used as qf; otherwise default QUERY_FIELDS are used.
     */
    public SearchResult search(com.mysolrproject.dto.SearchRequest request) {
        String q = request.effectiveQuery();
        int rows = request.effectiveRows();
        List<String> fieldsList = request.effectiveFields();
        String qf = (fieldsList != null && !fieldsList.isEmpty())
                ? String.join(" ", fieldsList)
                : QUERY_FIELDS;
        try {
            log.info("[SEARCH] Building Solr query: defType=edismax, q='{}', qf='{}', rows={}", q, qf, rows);
            SolrQuery query = new SolrQuery();
            query.set("defType", "edismax");
            query.setQuery(q);
            query.set("qf", qf);
            query.setRows(rows);

            QueryResponse response = solrClient.query(query);
            SolrDocumentList results = response.getResults();
            long numFound = results.getNumFound();
            long qTime = response.getElapsedTime();

            List<Map<String, Object>> documents = new ArrayList<>();
            for (SolrDocument doc : results) {
                documents.add(toFlatMap(doc));
            }

            log.info("[SEARCH] Solr response: numFound={}, returned={}, QTime={}ms", numFound, documents.size(), qTime);
            if (!documents.isEmpty()) {
                Map<String, Object> first = documents.get(0);
                log.info("[SEARCH] Data sample (first doc field count={}, keys={})", first.size(), first.keySet());
            }
            return new SearchResult(numFound, documents);
        } catch (Exception e) {
            log.error("[SEARCH] Search failed for q='{}'", q, e);
            throw new RuntimeException("Search failed: " + e.getMessage(), e);
        }
    }

    /**
     * Legacy: search by q and rows only (used if needed elsewhere).
     */
    public SearchResult search(String q, int rows) {
        return search(new com.mysolrproject.dto.SearchRequest(q, rows, null));
    }

    private static Map<String, Object> toFlatMap(SolrDocument doc) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (String fieldName : doc.getFieldNames()) {
            if ("_version_".equals(fieldName) || "_root_".equals(fieldName)) {
                continue;
            }
            out.put(fieldName, doc.getFirstValue(fieldName));
        }
        return out;
    }
}
