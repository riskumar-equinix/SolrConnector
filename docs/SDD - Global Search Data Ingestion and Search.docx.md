# SDD – Global Search: Data Ingestion and Search

---

## Table of Contents

1. [Document History](#document-history)
2. [Overview](#overview)
3. [Terminology & Abstraction](#terminology--abstraction)
4. [Approach Evolution](#approach-evolution)
5. [Current State Summary](#current-state-summary)
6. [Target Architecture](#target-architecture)
7. [Requirements](#requirements)
8. [System Design](#system-design)
9. [References](#references)

---

## Document History

| Version | Review Date | Changes | Review Outcome |
|---------|-------------|---------|----------------|
| 1.0 | TBD | Initial Design | Focus on global search architecture with dual data sources (Siebel, Activity Manager), canonical Solr schema, and unified search API. |

---

## Overview

IBX Operations runs on two systems today — Siebel, which has been around for years and holds most of the operational data, and Activity Manager, a newer PostgreSQL-based application that manages activity data in its own way. Both are important, but they don't talk to each other, and there's no easy way to search across both at once.

Right now, if someone wants to find an activity, an order, or an asset, they need to know which system it lives in before they even start looking. That's a problem. It slows things down and makes it harder to build features in Jarvis that need data from both places.

The idea behind this project is straightforward — pull data from both Siebel and Activity Manager into a single Solr index, give every document a common structure, and expose one search API that Jarvis can call. Users type a query, and they get results back without needing to care where the data came from.

This document covers how we're planning to build that — the approach, the data flows, what's already done, and what still needs to happen.

---

## Terminology & Abstraction

| Term | Definition |
|------|------------|
| **Siebel** | The legacy Oracle CRM — where most of the operational data (activities, orders, assets, accounts) currently lives. |
| **Activity Manager** | The newer PostgreSQL-based app managing activity data. Has its own schema, unrelated to Siebel. |
| **Canonical Schema** | The common field structure we use in Solr so docs from both sources look the same. |
| **Solr** | Apache Solr — the search engine at the core of this solution. Handles indexing and querying. |
| **SolrJ** | The Java client library for talking to Solr. |
| **edismax** | Solr extended query parser. Lets you search across multiple fields with different weights. |
| **Migration** | The process of reading records from a source DB and writing them into Solr. |
| **Indexer** | The service that handles migration for a specific data source. |
| **Source** | A field on every Solr document (`siebel` or `activity_manager`) that says where the data came from. |

---

## Approach Evolution

The way we get to global search isn't a single jump — it's three phases. Each one builds on the last and moves us further away from the current DIH dependency.

### Approach 1 – Current State (Siebel → DIH, Jarvis via GraphQL)

**Participants:** Siebel, DIH, Jarvis Client

- Siebel pushes its data into DIH, and Jarvis reads from DIH using GraphQL.
- Works fine for Siebel data, but Activity Manager isn't part of this picture at all.
- Everything runs through DIH — if it goes down, search goes down with it.

---

### Approach 2 – Interim State (Siebel + Activity Manager → DIH, Jarvis via GraphQL)

**Participants:** Siebel, Activity Manager, DIH, Jarvis Client

- DIH gets extended to also pull from Activity Manager, so both data sources are now in there.
- Jarvis doesn't need to change — it's still the same GraphQL call to DIH.
- The downside is that DIH is doing more work and is still the single point that everything depends on. Adding another data source later would mean changing DIH again.

---

### Approach 3 – Target State (Siebel + Activity Manager → Solr, Jarvis via REST)

**Participants:** Siebel, Activity Manager, Solr, Jarvis Client

- Both Siebel and Activity Manager feed into a single Solr index independently — no DIH in the middle.
- Every document gets mapped to the same field structure (canonical schema), so Jarvis doesn't need to handle two different formats.
- A `source` field on each document tells you where it came from — useful for filtering or showing a badge in the UI.
- Jarvis calls a REST search API instead of GraphQL.
- If we ever need to add another data source, it's just a new indexer — nothing else changes.

---

### Evolution Summary

| | Approach 1 (Current) | Approach 2 (Interim) | Approach 3 (Target) |
|---|---|---|---|
| **Siebel data in search** | Yes | Yes | Yes |
| **Activity Manager data in search** | No | Yes | Yes |
| **Search interface** | GraphQL (DIH) | GraphQL (DIH) | REST (Solr) |
| **DIH dependency** | Yes | Yes | No |
| **Canonical schema** | No | No | Yes |
| **Scalable to more sources** | No | Limited | Yes |

---

## Current State Summary

### Implemented: Siebel-to-Solr Migration

**Data Flow (Siebel → Solr)**

1. **Trigger:** Call `POST /api/migrate` — this kicks off `SolrIndexerService.runMigration()`.
2. **Oracle Connection:** SolrService opens a connection to the Siebel Oracle DB using credentials in `application.properties`.
3. **Query Execution:** Runs `SEARCHABLE_TASK_QUERY` — a paginated SQL query that joins several Siebel tables (`S_EVT_ACT`, `S_ORDER`, `S_ASSET`, `S_ORG_EXT`, etc.) to get activity, order, and asset data in one shot.
4. **Pagination:** Pulls 500 rows at a time using `OFFSET/FETCH` — needed because Siebel tables can be large.
5. **Document Mapping:** Each row becomes a `SolrInputDocument`. Column names map directly to field names. Rows with a non-numeric CSN are skipped.
6. **Batch Indexing:** Docs go to Solr in batches of 500. If a batch fails, it retries; on second failure, falls back to single-doc indexing so bad records don't block the rest.
7. **Commit:** All docs are committed to Solr when the migration finishes.
8. **Response:** Returns a `MigrationResult` — how many docs indexed, how many skipped, how long it took.

**Search Flow**

1. **Trigger:** `GET /api/search?q=...&rows=...` for quick queries, or `POST /api/search` with a `SearchRequest` body for more control.
2. **Query Building:** Builds a `SolrQuery` using `defType=edismax`. The `qf` (query fields) parameter controls which fields get searched and with what weight.
3. **Default Query Fields:** `ACTIVITYNUMBER`, `ORDERNUMBER`, `SALESORDERNUMBER`, `ACCOUNTNAME`, `CSN`, `IBXNAME`, `ACTSYSTEMNAME`, `ORDERSYSTEMNAME`, `SERIALNUMBER`, `ASSETNUMBER`.
4. **Execution:** Runs the query against Solr. Each `SolrDocument` in the result is converted to a flat map for the response.
5. **Response:** Returns a `SearchResult` with `numFound` and the list of documents.

### Not Yet Implemented: Activity Manager

- PostgreSQL connection and configuration still need to be wired up.
- The Activity Manager indexer service doesn't exist yet.
- Field mapping from Activity Manager entities to the canonical Solr schema is TBD.

---

## System Design Flows

There are three main flows — getting data in the first time, keeping it up to date, and actually searching it. Here's how each one works.

---

### Flow 1 – Initial Load

The initial load is manual — someone triggers it to seed Solr for the first time.

- For **Siebel**, SolrService connects to Oracle and runs a paginated query that pulls activities, orders, and assets in batches of 500. Each row gets mapped to the canonical schema and pushed to Solr with `source=siebel`.
- For **Activity Manager**, it does the same thing against PostgreSQL — paginated fetch, schema mapping, batch index with `source=activity_manager`.
- If a batch fails, it retries once. If that also fails, it drops down to one-doc-at-a-time mode so we don't lose the whole batch over one bad record.
- Both sources land in the same Solr core, so they're immediately queryable together.
- At the end, you get a summary — how many docs indexed, how many skipped, and how long it took.

---

### Flow 2 – Delta Load

Once the initial load is done, we need to keep Solr current. The two sources handle this differently.

**Siebel (runs every 5 minutes):**

- A scheduler wakes up every 5 minutes.
- It checks Solr for the most recent `lastUpdated` timestamp among existing Siebel docs.
- Then it queries Siebel for anything that's changed since that timestamp.
- Those records get upserted into Solr — so if a doc already exists it gets updated, if it's new it gets added.

**Activity Manager (Kafka-based, near real-time):**

- Activity Manager publishes events to a Kafka topic whenever something changes.
- On a **CREATE event**, SolrService adds a new document to Solr.
- On an **UPDATE event**, SolrService does a partial update — only the fields that changed, not a full re-index of the document.
- This keeps the lag very low without hammering the database.

---

### Flow 3 – Search

- Jarvis sends a search query to SolrService — either a free-text string or a structured request with filters.
- Supported filters: IBX, activity type, date range. Pagination is also supported (page number + page size).
- Default sort is by last updated date, newest first.
- SolrService runs the query against the unified Solr core and returns results from both Siebel and Activity Manager in one response.
- The response includes the matched documents, total result count, total pages, and facets broken down by source, IBX, and activity type.
- Each document has a `source` field so the UI can show where it came from if needed.

---

## Target Architecture

### System Context Diagram

```
┌─────────────────────────┐     ┌─────────────────────────┐
│        Siebel           │     │    Activity Manager     │
│       (Oracle)          │     │     (PostgreSQL)        │
│  Own entity structure   │     │  Own entity structure   │
│  (S_EVT_ACT, S_ORDER,   │     │  (tables/entities)       │
│   S_ASSET, etc.)        │     │                         │
└────────┬────────────────┘     └────────┬────────────────┘
         │                               │
         │  Transform/Map                 │  Transform/Map
         │  to canonical schema           │  to canonical schema
         │                               │
         ▼                               ▼
         └───────────────┬───────────────┘
                         │
                         ▼
         ┌───────────────────────────────────┐
         │   Apache Solr (oracle-core)       │
         │   Unified canonical schema        │
         │   + source field (siebel | am)    │
         └───────────────────────────────────┘
                         │
                         ▼
         ┌───────────────────────────────────┐
         │   Global Search API                │
         │   GET/POST /api/search             │
         │   POST /api/migrate (both sources) │
         └───────────────────────────────────┘
```

### Canonical Schema (Proposed)

The goal is that a document from Siebel and a document from Activity Manager look the same to Solr. Both indexers map their source fields into this shared set of field names.

| Field | Type | Description | Siebel Source | Activity Manager Source |
|-------|------|-------------|---------------|-------------------------|
| `id` | string | Unique document ID | `siebel_<ROW_ID>` | `am_<id>` |
| `source` | string | Data origin | `siebel` | `activity_manager` |
| `entityType` | string | Entity type (activity, order, asset, etc.) | Mapped from Siebel | Mapped from AM |
| `activityNumber` | string | Activity identifier | ACTIVITYNUMBER | AM equivalent |
| `orderNumber` | string | Order identifier | ORDERNUMBER | AM equivalent |
| `accountName` | string | Account name | ACCOUNTNAME | AM equivalent |
| `csn` | string | Customer/Account number | CSN | AM equivalent |
| `ibxName` | string | IBX name | IBXNAME | AM equivalent |
| `systemName` | string | System/cage identifier | ACTSYSTEMNAME / ORDERSYSTEMNAME | AM equivalent |
| `serialNumber` | string | Serial number | SERIALNUMBER | AM equivalent |
| `assetNumber` | string | Asset number | ASSETNUMBER | AM equivalent |
| ... | ... | Additional canonical fields | ... | ... |

*The exact field list will be locked down once we've mapped the Activity Manager schema and confirmed what fields need to be searchable.*

---

## Requirements

1. **Dual-source ingestion:** Both Siebel (Oracle) and Activity Manager (PostgreSQL) need to feed into the same Solr core.

2. **Canonical schema:** Every document in Solr should follow the same field structure, regardless of where it came from. This is what makes unified search possible.

3. **Source tracking:** Each document needs a `source` field so we can filter by Siebel or Activity Manager when needed.

4. **Unique document IDs:** IDs must be globally unique across both sources — prefixing with `siebel_` or `am_` handles this cleanly and avoids collisions.

5. **Unified search API:** One API endpoint, one response — results from both systems together.

6. **Per-source migration control:** It should be possible to trigger migration for Siebel only, Activity Manager only, or both. Useful for incremental rollouts and debugging.

7. **Pagination and batching:** Both the DB reads and the Solr writes need to be chunked. Siebel tables can be large, and Solr performs better with batch commits than one-by-one indexing.

---

## System Design

### Component Overview

| Component | Responsibility |
|-----------|----------------|
| **OracleDBConnector** | Provides Oracle (Siebel) database connections. |
| **PostgreSQLConnector** *(to be added)* | Provides PostgreSQL (Activity Manager) database connections. |
| **SolrIndexerService** | Migrates Siebel data to Solr; maps Siebel columns to canonical schema. |
| **ActivityManagerIndexerService** *(to be added)* | Migrates Activity Manager data to Solr; maps AM entities to canonical schema. |
| **SolrSearchService** | Executes search queries against Solr using edismax. |
| **MigrationController** | REST API for migration and search. |
| **SolrConfig** | Configures Solr client bean. |

### Migration Flow (Target)

**Siebel migration (existing):**
1. `POST /api/migrate/siebel` → triggers `SolrIndexerService.runMigration()`
2. Connect to Oracle, run paginated Siebel query
3. Map each row to the canonical schema, set `id` and `source`
4. Batch-add to Solr, then commit

**Activity Manager migration (to be implemented):**
1. `POST /api/migrate/activity-manager` → triggers `ActivityManagerIndexerService.runMigration()`
2. Connect to PostgreSQL, run paginated query over AM tables
3. Same mapping step, different field names
4. Batch-add to Solr, then commit

**Combined migration:**
1. `POST /api/migrate` → runs both indexers back to back

### Search Flow (Existing)

1. Client calls `GET /api/search?q=...&rows=...` or `POST /api/search` with a `SearchRequest` body
2. `SolrSearchService` builds an edismax query targeting the canonical searchable fields
3. Solr runs the query and returns matching docs
4. Results are packaged into a `SearchResult` (numFound + document list)
5. Caller can also pass `source=siebel` or `source=activity_manager` as a filter if they only want results from one side

### Configuration

**application.properties (current):**
```properties
server.port=8080
oracle.db.url=jdbc:oracle:thin:@//...
oracle.db.username=...
oracle.db.password=...
solr.url=http://localhost:8983/solr/oracle-core
```

**To be added for Activity Manager:**
```properties
postgres.db.url=jdbc:postgresql://...
postgres.db.username=...
postgres.db.password=...
```

### Technology Stack

| Component | Technology |
|-----------|------------|
| Framework | Spring Boot 3.2.5 |
| Java | 17 |
| Build | Maven |
| Siebel DB | Oracle JDBC (ojdbc8) |
| Activity Manager DB | PostgreSQL JDBC *(to be added)* |
| Search | Apache Solr 9.10.1, SolrJ client |

---

## References

- [Apache Solr Documentation](https://solr.apache.org/guide/)
- [SolrJ API](https://solr.apache.org/docs/latest/solr-solrj/)
- Project: `/Users/riskumar/GitHub/Solr-POC/SolrConnector`
