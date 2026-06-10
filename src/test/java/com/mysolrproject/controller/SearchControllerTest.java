package com.mysolrproject.controller;

import com.mysolrproject.dto.SearchResult;
import com.mysolrproject.service.SolrSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchController.class)
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SolrSearchService searchService;

    @Test
    void getSearch_returns200_withResults() throws Exception {
        when(searchService.search(any())).thenReturn(new SearchResult(2L, List.of()));

        mockMvc.perform(get("/api/search").param("q", "Equinix").param("rows", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numFound").value(2));
    }
}
