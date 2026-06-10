# Global Search – Sequence Diagrams for SequenceDiagram.org

Paste each source block separately into [https://sequencediagram.org](https://sequencediagram.org)

---

## Diagram 1 – Initial Load

```
title Initial Load

actor Admin
participant SolrService
database Siebel
database ActivityManager
database Solr

== Initial Load - Siebel ==

Admin->SolrService: Trigger Siebel migration
SolrService->Siebel: Fetch all records (paginated)
Siebel-->SolrService: Records
SolrService->Solr: Batch index (source=siebel)
Solr-->SolrService: Indexed
SolrService-->Admin: Migration complete

== Initial Load - Activity Manager ==

Admin->SolrService: Trigger AM migration
SolrService->ActivityManager: Fetch all records (paginated)
ActivityManager-->SolrService: Records
SolrService->Solr: Batch index (source=activity_manager)
Solr-->SolrService: Indexed
SolrService-->Admin: Migration complete
```

---

## Diagram 2 – Delta Load

```
title Delta Load

participant Scheduler
participant SolrService
participant Kafka
database Siebel
database ActivityManager
database Solr

== Delta Load - Siebel ==

Scheduler->SolrService: Trigger every 5 min
SolrService->Solr: Get last indexed timestamp
Solr-->SolrService: Timestamp
SolrService->Siebel: Fetch records changed since timestamp
Siebel-->SolrService: Changed records
SolrService->Solr: Upsert changed records
Solr-->SolrService: Updated

== Delta Load - Activity Manager ==

ActivityManager->Kafka: Publish CREATE event (new record)
Kafka->SolrService: Deliver CREATE event
SolrService->Solr: Add new record (source=activity_manager)
Solr-->SolrService: Indexed

ActivityManager->Kafka: Publish UPDATE event (changed fields)
Kafka->SolrService: Deliver UPDATE event
SolrService->Solr: Partial update existing record
Solr-->SolrService: Updated
```

---

## Diagram 3 – Search

```
title Search

actor Jarvis
participant SolrService
database Solr

Jarvis->SolrService: Search query (text, filters, page, sort)
SolrService->Solr: Query index
Solr-->SolrService: Results (Siebel + AM)
SolrService-->Jarvis: Response (results, facets, pagination)
```
