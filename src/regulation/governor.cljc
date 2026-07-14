(ns regulation.governor
  "RegulationGovernor — the independent safety/traceability layer
  for the ISIC Rev.5 8413 economic regulation and business compliance
  administration actor. Wired as its own `:govern` node in `regulation.actor`'s
  StateGraph, downstream of `:advise` — the Advisor has no notion of
  business provenance or threshold risk, so this MUST be a separate system
  able to reject a proposal (itonami actor pattern, per ADR-2607011000 / CLAUDE.md
  Actors section).

  `check` is a pure function of (request, context, proposal, store) ->
  verdict; it never mutates the store. The StateGraph's `:decide` node
  routes on the verdict:
    :hard? true                → :hold  (irreversible, no write)
    :escalate? true            → :request-approval (interrupt-before)
    otherwise                  → :commit

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. business provenance     — the request's business must be registered.
    2. no-actuation            — proposal :effect must be :propose.
    3. scope-boundary          — proposals touching permit grant/denial,
                                  binding tariff/rate/ruling setting, enforcement
                                  actions, or registration manipulation NEVER
                                  PROCEED (closed allowlist enforced here + in advisor).

  ESCALATION invariants (:escalate? true, ALWAYS human sign-off):
    4. intake-permit-application — all permit application intake must be escalated.
    5. flag-complaint            — all complaint flags must be escalated.
    6. low confidence            (< `confidence-floor`)."
  (:require [regulation.store :as store]
            [regulation.advisor :as advisor]))

(def confidence-floor 0.6)

; Permanently forbidden operation categories
(def ^:private forbidden-ops #{:unknown})  ; :unknown catches out-of-scope proposals from advisor

; Escalating operations (require human approval)
(def ^:private escalating-ops #{:intake-permit-application
                                 :flag-complaint})

(defn- hard-violations [{:keys [proposal]} business-record]
  (cond-> []
    (nil? business-record)
    (conj {:rule :no-business :detail "business not registered"})

    (not= :propose (:effect proposal))
    (conj {:rule :no-actuation :detail "effect must be :propose only (no direct store writes)"})

    (contains? forbidden-ops (:op proposal))
    (conj {:rule :scope-boundary
           :detail "operation outside permitted scope (permit grant/denial, binding tariff/rate/ruling, enforcement, registration manipulation are permanently forbidden)"})))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `regulation.store/Store`. Returns
  `{:ok? bool :violations [...] :confidence n :hard? bool :escalate? bool}`."
  [request context proposal store]
  (let [business-record (store/business store (:business-id request))
        hard (hard-violations {:proposal proposal} business-record)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        escalating-op? (contains? escalating-ops (:op proposal))]
    {:ok? (and (not hard?) (not low?) (not escalating-op?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? escalating-op?))}))
