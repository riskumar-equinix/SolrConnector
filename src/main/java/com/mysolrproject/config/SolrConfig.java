package com.mysolrproject.config;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SolrConfig {

    private static final Logger log = LoggerFactory.getLogger(SolrConfig.class);

    @Bean
    public SolrClient solrClient(@Value("${solr.url}") String solrUrl) {
        log.info("[CONFIG] Solr client configured - url={}", solrUrl);
        return new HttpJdkSolrClient.Builder(solrUrl).build();
    }
}
