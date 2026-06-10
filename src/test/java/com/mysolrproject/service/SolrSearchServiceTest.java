package com.mysolrproject.service;

import com.mysolrproject.dto.SearchRequest;
import com.mysolrproject.dto.SearchResult;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolrSearchServiceTest {

    @Mock
    private SolrClient solrClient;

    @Mock
    private QueryResponse queryResponse;

    private SolrSearchService searchService;

    @BeforeEach
    void setUp() {
        searchService = new SolrSearchService(solrClient);
    }

    @Test
    void search_returnsEmptyResults_whenSolrReturnsNoDocuments() throws Exception {
        SolrDocumentList emptyList = new SolrDocumentList();
        when(solrClient.query(any())).thenReturn(queryResponse);
        when(queryResponse.getResults()).thenReturn(emptyList);
        when(queryResponse.getElapsedTime()).thenReturn(5L);

        SearchResult result = searchService.search(new SearchRequest("test", 10, null));

        assertThat(result.numFound()).isZero();
        assertThat(result.documents()).isEmpty();
    }

    @Test
    void searchRequest_effectiveQuery_defaultsToWildcard_whenBlank() {
        SearchRequest req = new SearchRequest(null, null, null);
        assertThat(req.effectiveQuery()).isEqualTo("*:*");
    }

    @Test
    void searchRequest_effectiveRows_defaultsTen_whenNull() {
        SearchRequest req = new SearchRequest("q", null, null);
        assertThat(req.effectiveRows()).isEqualTo(10);
    }

    @Test
    void searchRequest_effectiveRows_capsAtThousand() {
        SearchRequest req = new SearchRequest("q", 9999, null);
        assertThat(req.effectiveRows()).isEqualTo(1000);
    }
}
