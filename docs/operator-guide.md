# Operator Guide: Running the RegulationActor

## Overview

This guide is for regulatory agency staff, IT operators, and system administrators who deploy and operate the RegulationActor on their infrastructure.

The actor assists with:
- Business registration verification and intake
- Permit application intake and review
- Economic statistics collection and logging
- Business compliance complaint escalation

The actor **never** makes binding decisions. All permit grants/denials, tariff/rate setting, binding rulings, and enforcement actions remain human-only.

## Prerequisites

- **Clojure** 1.11+, installed locally or via Docker.
- **JVM** 11+ (if running natively) or a Docker image with Clojure.
- **Business registry database** (read-only): a PostgreSQL, Datomic, or file-based registry of known businesses.
- **Human approval workflow**: staff to review and approve escalated cases via a robotics safety console (web UI).

## Installation

### Option 1: Clone and Run Locally

```bash
git clone https://github.com/cloud-itonami/cloud-itonami-isic-8413.git
cd cloud-itonami-isic-8413

# Test the default setup (in-memory store, mock advisor)
clojure -M:test

# Run interactively (REPL)
clojure
```

### Option 2: Docker

```dockerfile
FROM clojure:latest
WORKDIR /app
COPY . .
RUN clojure -M:test
CMD ["clojure", "-M:run"]
```

## Configuration

### Store Integration

By default, the actor uses an in-memory store (`regulation.store/mem-store`). To integrate with your business registry:

1. Implement the `regulation.store/Store` protocol for your database:
   ```clojure
   (defprotocol Store
     (business [s business-id])
     (records-of [s business-id])
     (ledger [s])
     (register-business! [s business])
     (commit-record! [s record])
     (append-ledger! [s fact]))
   ```

2. For example, with PostgreSQL:
   ```clojure
   (defrecord PGStore [db-conn]
     Store
     (business [_ business-id]
       (jdbc/query db-conn ["SELECT * FROM businesses WHERE id = ?" business-id]))
     ;; ... etc
   )
   ```

3. Pass your store to the actor graph:
   ```clojure
   (regulation.actor/build-graph {:store (pg-store db-conn)})
   ```

### Advisor Customization

#### Mock Advisor (Default)

The `mock-advisor` is deterministic and suitable for testing:

```clojure
(regulation.advisor/mock-advisor)
```

It reads the request's `:op` (operation) and `:stake` fields and returns a deterministic proposal with high confidence.

#### LLM Advisor

To use a real LLM (e.g., OpenAI, Anthropic), implement:

```clojure
(regulation.advisor/llm-advisor chat-model model-generate-fn gen-opts)
```

where `chat-model` is a `langchain.model/ChatModel` instance.

Example with Anthropic:
```clojure
(require '[langchain.model.chat :as chat])

(let [model (chat/anthropic-chat-model {:model "claude-3-sonnet"
                                        :api-key (System/getenv "ANTHROPIC_API_KEY")})
      advisor (regulation.advisor/llm-advisor model chat/-generate {})]
  (regulation.actor/build-graph {:store store :advisor advisor}))
```

## Running a Request

### Manual / Programmatic API

```clojure
(require '[regulation.actor :as actor]
         '[regulation.store :as store]
         '[regulation.advisor :as advisor])

;; Build the actor graph
(def graph (actor/build-graph {:store (store/mem-store)
                                :advisor (advisor/mock-advisor)}))

;; Define a regulatory request
(def request {:business-id "biz-123"
              :op :intake-permit-application
              :stake :medium})

;; Run the request
(def result (actor/run-request! graph request {} "thread-001"))

;; Result contains:
;; {:state {... :disposition :request-approval ...}
;;  :status :interrupted or :done
;;  :events [...]}

;; If :request-approval (interrupted for human review), staff reviews and approves:
(def approved (actor/approve! graph "thread-001"))
```

### Web API (via Robotics Safety Console)

The actor is typically accessed via a web console built with `kotoba.robotics.ui`. The console:

1. Displays incoming requests (permit applications, complaints, registration verifications).
2. Shows advisor recommendations and confidence scores.
3. Displays the governor's audit trail and verdict reasoning.
4. Provides human approval/rejection UI for escalated cases.
5. Logs all decisions to the audit ledger.

See [`docs/samples/operator-console.html`](samples/operator-console.html) for a live example.

## Monitoring and Audit

### Ledger Queries

All decisions are logged to an append-only ledger. Query it:

```clojure
(store/ledger store)
;; Returns: [{:node :advise :request {...} :proposal {...}}
;;           {:node :govern :verdict {...}}
;;           {:disposition :commit :record {...}}
;;           ...]
```

### Record Queries

Query committed records by business:

```clojure
(store/records-of store "biz-123")
;; Returns: [{:business-id "biz-123" :op :intake-permit-application :payload {...}}
;;           ...]
```

### Health Checks

- **Advisor latency**: time to generate a proposal (especially important for LLM-based advisors).
- **Governor latency**: time to audit a proposal (typically < 1ms, pure function).
- **Ledger size**: monitor append-only ledger size (should grow linearly).
- **Outstanding escalations**: count of interrupted requests awaiting human approval.

## Troubleshooting

### "business not registered" (hard error)

The request's `:business-id` is not in the registry. Solutions:

1. Verify the business ID is correct.
2. Add the business to the registry: `(store/register-business! store {...})`.
3. Restart the request.

### "effect must be :propose only" (hard error)

This should never happen in normal operation (the advisor always assigns `:effect :propose`). If it does:

1. Check for custom advisor implementations that assign other effects.
2. Revert to the mock advisor temporarily to isolate the issue.

### "operation not in permitted allowlist" (hard error)

The advisor (or custom code) proposed an out-of-scope operation. Permitted operations are:
- `:verify-business-registration`
- `:intake-permit-application`
- `:log-economic-statistic`
- `:flag-complaint`

### Low Advisor Confidence (escalation)

If the advisor returns `:confidence < 0.6`, the request is automatically escalated for human review. This is by design. Solutions:

1. Review the advisor's `:rationale` field.
2. For LLM advisors, consider:
   - Better prompt engineering (in `regulation.advisor`'s system-prompt).
   - Retraining or fine-tuning the model.
   - Providing more context to the advisor (richer request structure).

## Backup and Recovery

### Ledger Backup

Since the ledger is append-only and immutable, backing it up is straightforward:

```bash
# Example: dump ledger to JSON
clojure -e "(println (json/write-str (store/ledger @store-atom)))" > ledger-backup.json
```

### Records Backup

```bash
# Dump all records by business
clojure -e "(doseq [bid (keys (:businesses @store-atom))]
              (println (store/records-of store bid)))" > records-backup.edn
```

### Disaster Recovery

If the in-memory store is lost, rebuild it from the ledger:

```clojure
(let [ledger-entries (read-file "ledger-backup.json")
      store (store/mem-store)]
  ;; Replay the ledger to reconstruct state
  (doseq [entry ledger-entries]
    (if (= :commit (:disposition entry))
      (store/commit-record! store (:record entry))
      (store/append-ledger! store entry))))
```

## Performance Tuning

### Advisor Latency

- **Mock advisor**: < 1ms (pure function).
- **LLM advisor**: 100ms–5s (depends on model and network).

To optimize:
- Batch requests if possible.
- Use a faster model (e.g., GPT-3.5 instead of GPT-4) if accuracy permits.
- Cache frequent advisories if the request patterns are repetitive.

### Governor Latency

- Always < 1ms (pure function, no I/O).
- No tuning needed.

### Store Latency

Depends on the backend:
- **MemStore**: < 1ms (in-memory).
- **PostgreSQL**: 10–100ms (depends on indexes, load).
- **Datomic**: 50–500ms (depends on server and query complexity).

Index the ledger and records tables for fast queries:

```sql
CREATE INDEX idx_businesses_id ON businesses (id);
CREATE INDEX idx_records_business_id ON records (business_id);
CREATE INDEX idx_ledger_timestamp ON ledger (timestamp DESC);
```

## Support and Contributions

For issues, questions, or contributions:

- **Repository**: https://github.com/cloud-itonami/cloud-itonami-isic-8413
- **License**: AGPL-3.0-or-later
- **Related**: cloud-itonami-isic-8422 (defence procurement blueprint)
