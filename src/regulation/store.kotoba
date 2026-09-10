(ns regulation.store
  "SSoT for the ISIC Rev.5 8413 economic regulation and business compliance
  administration actor. Store is a protocol injected into the
  `regulation.actor` StateGraph — `MemStore` is the default, deterministic,
  zero-dep backend; a Datomic/kotoba-server-backed implementation can be
  swapped in without touching the actor or governor (itonami actor
  pattern, per ADR-2607011000 / CLAUDE.md's Actors section).

  Domain:

    business  — a registered business entity (:business-id, :name, :registration-date, :verified-at)
    record    — a committed compliance or regulatory operating record
                (registration verification, permit application intake, economic
                data logging, complaint escalation) — written ONLY via
                commit-record!, never mutated in place
    ledger    — an append-only audit trail of every proposal/verdict/
                disposition, regardless of outcome (commit or hold)")

(defprotocol Store
  (business [s business-id])
  (records-of [s business-id])
  (ledger [s])
  (register-business! [s business])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (business [_ business-id] (get-in @a [:businesses business-id]))
  (records-of [_ business-id] (filter #(= business-id (:business-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-business! [s business]
    (swap! a assoc-in [:businesses (:business-id business)] business) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:businesses {} :records [] :ledger []} seed)))))
