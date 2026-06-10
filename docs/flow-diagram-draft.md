# SolrConnector – Flow Diagram

> Status: FINAL DRAFT

---

## Architecture Decisions

| Topic | Decision |
|-------|----------|
| Solr cores | **Single core** for both Siebel and Activity Manager |
| Siebel initial load | Manual trigger – `POST /api/migrate/siebel` |
| Siebel incremental | Scheduler every 5 min – delta via last indexed timestamp |
| AM initial load | Manual trigger – `POST /api/migrate/activity-manager` |
| AM incremental | Kafka event-driven – partial field updates |
| Delta timestamp source | Query Solr for last indexed record's `LASTUPDATED` field |
| Search | All fields, all users, paginated, filterable, with facets |
| Deletes | Not in scope |
| DB failure | Retry 3 times then fail |
| Solr batch failure | Retry batch → single-doc fallback |

---

## Flow Diagrams

---

### 1. Siebel – Initial Full Load

```
  Admin / CI
      │
      ▼
  POST /api/migrate/siebel
      │
      ▼
  ┌─────────────────────────────────────────┐
  │         SolrIndexerService              │
  │                                         │
  │  1. Connect to Oracle (retry x3)        │
  │  2. Run paginated query                 │
  │     OFFSET 0, FETCH 500                 │
  │  3. For each row:                       │
  │     - Validate (skip invalid CSN)       │
  │     - Map to canonical schema           │
  │     - Add source = "siebel"             │
  │     - Add id = "siebel_<ROW_ID>"        │
  │  4. Batch 500 docs → Solr (retry x3)   │
  │     └─ on fail → single-doc fallback   │
  │  5. Repeat until last page              │
  │  6. Commit                              │
  └─────────────────────────────────────────┘
      │
      ▼
  MigrationResult
  (indexed, skipped, elapsedMs)
```

---

### 2. Siebel – Incremental Sync (every 5 min)

```
  Scheduler (every 5 min)
      │
      ▼
  ┌─────────────────────────────────────────────┐
  │           SolrIndexerService                │
  │                                             │
  │  1. Query Solr for last indexed record      │
  │     → GET max(LASTUPDATED) from siebel-docs │
  │                                             │
  │  2. Query Oracle:                           │
  │     WHERE LASTUPDATED > <lastTimestamp>     │
  │     ORDER BY LASTUPDATED ASC                │
  │                                             │
  │  3. For each changed row:                   │
  │     - Map to canonical schema               │
  │     - Upsert into Solr (by id)              │
  │                                             │
  │  4. Commit                                  │
  └─────────────────────────────────────────────┘
```

---

### 3. Activity Manager – Initial Full Load

```
  Admin / CI
      │
      ▼
  POST /api/migrate/activity-manager
      │
      ▼
  ┌─────────────────────────────────────────┐
  │     ActivityManagerIndexerService       │
  │                                         │
  │  1. Connect to PostgreSQL (retry x3)    │
  │  2. Run paginated query                 │
  │     OFFSET 0, LIMIT 500                 │
  │  3. For each row:                       │
  │     - Map to canonical schema           │
  │     - Add source = "activity_manager"   │
  │     - Add id = "am_<id>"               │
  │  4. Batch 500 docs → Solr (retry x3)   │
  │     └─ on fail → single-doc fallback   │
  │  5. Repeat until last page              │
  │  6. Commit                              │
  └─────────────────────────────────────────┘
      │
      ▼
  MigrationResult
  (indexed, skipped, elapsedMs)
```

---

### 4. Activity Manager – Event-Driven Incremental (Kafka)

```
  Activity Manager
      │
      │── publishes partial update event ──▶  Kafka Topic
                                                   │
                                                   ▼
                                         ┌──────────────────────────┐
                                         │   Kafka Consumer         │
                                         │  (SolrConnector)         │
                                         │                          │
                                         │  1. Read event           │
                                         │     (changed fields only)│
                                         │  2. Map to canonical     │
                                         │     schema (partial)     │
                                         │  3. Partial update doc   │
                                         │     in Solr by id        │
                                         │  4. Commit               │
                                         └──────────────────────────┘
```

---

### 5. Search Flow

```
  Jarvis Client
      │
      ▼
  GET /api/search
  ?q=<query>
  &rows=20&page=1
  &fq=IBXNAME:<ibx>
  &fq=ACTIVITYTYPE:<type>
  &fq=LASTUPDATED:[<from> TO <to>]
      │
      ▼
  ┌────────────────────────────────────────────┐
  │           SolrSearchService                │
  │                                            │
  │  1. Build edismax query                    │
  │     qf = activityNumber, orderNumber,      │
  │           accountName, csn, ibxName,       │
  │           systemName, serialNumber         │
  │  2. Apply filters (fq)                     │
  │  3. Apply pagination (start, rows)         │
  │  4. Request facets:                        │
  │     - facet.field = source                 │
  │     - facet.field = IBXNAME                │
  │     - facet.field = ACTIVITYTYPE           │
  │  5. Query single Solr core                 │
  │  6. Return all fields                      │
  └────────────────────────────────────────────┘
      │
      ▼
  SearchResult
  ┌──────────────────────────────────┐
  │  numFound: 1240                  │
  │  page: 1, rows: 20               │
  │  facets: {                       │
  │    source: { siebel: 900,        │
  │              activity_manager: 340 } │
  │    IBXNAME: { SY2: 400, ... }    │
  │    ACTIVITYTYPE: { Install: 700, │
  │                    Change: 540 } │
  │  }                               │
  │  documents: [ {...}, {...}, ... ] │
  └──────────────────────────────────┘
```

---

### 6. Error Handling

```
  DB Connection Failure
  ┌─────────────────────────────────────┐
  │  Attempt 1 → fail                   │
  │  Wait 2s → Attempt 2 → fail         │
  │  Wait 4s → Attempt 3 → fail         │
  │  Throw exception → return 500       │
  └─────────────────────────────────────┘

  Solr Batch Failure
  ┌─────────────────────────────────────┐
  │  Batch of 500 → fail                │
  │  Retry batch → fail                 │
  │  Fall back: send 1 doc at a time    │
  │  Log any rejected single docs       │
  │  Continue with next batch           │
  └─────────────────────────────────────┘
```

---

### 7. Search – Option A: Via SolrConnector REST API (Recommended for abstraction)

```
  Jarvis Frontend (Browser)
      │
      │── GET /api/search
      │   ?q=<free text>
      │   &rows=20&page=1
      │   &sort=LASTUPDATED desc
      │   &fq=IBXNAME:<ibx>
      │   &fq=ACTIVITYTYPE:<type>
      │   &fq=LASTUPDATED:[<from> TO <to>]
      │
      ▼
  SolrConnector (Spring Boot)
      │
      │── Build edismax query
      │── Apply filters + sort
      │── Request facets
      │── Query Solr core
      │
      ▼
  Solr (single unified core)
      │
      ▼
  SolrConnector assembles response:
  {
    "numFound": 1240,
    "currentPage": 1,
    "totalPages": 62,
    "pageSize": 20,
    "facets": {
      "source":       { "siebel": 900, "activity_manager": 340 },
      "IBXNAME":      { "SY2": 400, "AM2": 300, ... },
      "ACTIVITYTYPE": { "Install": 700, "Change": 540 }
    },
    "documents": [ {...}, {...}, ... ]
  }
      │
      ▼
  Jarvis Frontend
```

---

### 8. Search – Option B: Jarvis Calls Solr Directly (Simpler, less abstraction)

```
  Jarvis Frontend (Browser)
      │
      │── HTTP GET http://solr-host:8983/solr/oracle-core/select
      │   ?defType=edismax
      │   &q=<free text>
      │   &qf=ACTIVITYNUMBER ORDERNUMBER ACCOUNTNAME CSN IBXNAME ...
      │   &rows=20&start=0
      │   &sort=LASTUPDATED desc
      │   &fq=IBXNAME:<ibx>
      │   &facet=true
      │   &facet.field=source
      │   &facet.field=IBXNAME
      │   &facet.field=ACTIVITYTYPE
      │
      ▼
  Solr (single unified core)
      │
      ▼
  Raw Solr JSON response → Jarvis Frontend

  Trade-offs vs Option A:
  ┌──────────────────────────────────────────┐
  │ Pro: One less network hop, simpler stack │
  │ Pro: Jarvis controls query directly      │
  │ Con: Solr exposed to frontend            │
  │ Con: No abstraction layer for auth,      │
  │      field mapping, or future changes    │
  │ Con: Any Solr URL / schema changes       │
  │      require frontend changes            │
  └──────────────────────────────────────────┘
```

---

## Open Items

| # | Item | Notes |
|---|------|-------|
| 1 | Kafka topic name for AM events | Need from AM team |
| 2 | AM event schema (which fields in partial update) | Need from AM team |
| 3 | Siebel `LASTUPDATED` column name to confirm | Check Siebel schema |
| 4 | Canonical field names (camelCase vs UPPERCASE) | Decision needed |
| 5 | Solr core name (currently `oracle-core`) | Rename to `global-search`? |
