# Lucidchart Prompt – Global Search Flow Diagram

Paste the prompt below into Lucidchart's AI diagram generator.

---

## Prompt

Create a flow diagram for a **Global Search** system with the following participants and flows.

**Participants:** Admin, Scheduler, SolrService, Siebel (Oracle), Activity Manager (PostgreSQL), Kafka, Solr, Jarvis

---

**Flow 1 – Initial Load (one-time, triggered manually)**
- Admin triggers SolrService to load all data from Siebel into Solr
- Admin triggers SolrService to load all data from Activity Manager into Solr

**Flow 2 – Delta Load (ongoing)**
- Every 5 minutes, Scheduler triggers SolrService to fetch only changed records from Siebel and upsert them into Solr
- When Activity Manager creates or updates a record, it publishes an event to Kafka. SolrService consumes the event and updates Solr accordingly.

**Flow 3 – Search**
- Jarvis sends a search query to SolrService
- SolrService queries Solr and returns results back to Jarvis

---

**Layout notes:**
- Place Solr in the center
- Siebel and Activity Manager on the left feeding into Solr
- Jarvis on the right reading from Solr
- Use different colors for Initial Load, Delta Load, and Search flows
