# Global Search – Sequence Diagrams for SequenceDiagram.org

Paste any source block below into [https://sequencediagram.org](https://sequencediagram.org).

---

## 1. Current Approach (DIH + GraphQL)

```
title Global Search – Current Approach

participant "Siebel\n(Legacy)" as Siebel
participant "DIH\n(Data Integration Hub)" as DIH
participant "Jarvis Client" as Client

== Data Push ==

Siebel->DIH: Push data\n(activities, orders, assets)

== Search ==

Client->DIH: GraphQL query
DIH-->Client: Response
```

---

## 2. Interim Approach (DIH + Activity Manager + GraphQL)

```
title Global Search – Interim Approach

participant "Siebel\n(Legacy)" as Siebel
participant "Activity Manager\n(Next-Gen)" as AM
participant "DIH\n(Data Integration Hub)" as DIH
participant "Jarvis Client" as Client

== Data Ingestion ==

Siebel->DIH: Push legacy data\n(activities, orders, assets)
DIH-->Siebel: Acknowledged

DIH->AM: Query next-gen activity data
AM-->DIH: Return activity records
DIH->DIH: Index into DIH

== Search ==

Client->DIH: GraphQL query
DIH-->Client: Unified response\n(Siebel + Activity Manager data)
```

---

## 3. Proposed Approach (Global Search via Solr)

```
title Global Search – Proposed Approach

participant "Siebel\n(Legacy)" as Siebel
participant "Solr\n(Unified Index)" as Solr
participant "Jarvis Client" as Client

== Data Ingestion ==

Siebel->Solr: Index legacy data\n(activities, orders, assets)
Solr-->Siebel: Indexed

== Search ==

Client->Solr: Search query
Solr-->Client: Results
```
