# cloud-itonami-isic-8413

Open Occupation Blueprint for **ISIC Rev.5 8413**: Economic Regulation and Business Compliance Administration.

This repository designs a forkable OSS business for an economic regulatory agency's administrative support: a document-handling and verification robot performs business registration verification, permit application intake, economic data collection, and complaint escalation under a governor-gated actor, so a regulatory office keeps its own compliance records and audit trail instead of renting a closed regulatory SaaS.

## IMPORTANT: SCOPE BOUNDARIES

**This actor is EXPLICITLY NOT a regulatory decision authority or a binding permit/tariff/ruling issuer.**

### What this actor DOES

- Administrative and regulatory support operations only:
  - Business registration verification and record intake
  - Business permit application intake and documentation
  - Economic statistics data collection and logging
  - Business compliance complaint intake and escalation
  - Audit trail and compliance documentation

### What this actor DOES NOT (hard boundaries, permanently out of scope)

These operations are **permanently forbidden** — they are not gated by risk level or approval hierarchy, they cannot be escalated for human override, and the actor's proposal vocabulary has no path to construct them. A closed allowlist enforces this at the governance layer:

- **Binding permit grant or denial** — the actor NEVER grants, denies, or revokes a business permit or license; permit decisions remain exclusively human
- **Setting binding tariffs, rates, or fees** — no authority to set binding economic terms; rates/fees are human decisions only
- **Issuing binding regulatory rulings or directives** — no authority to issue binding interpretations of regulations; all rulings remain human domain
- **Economic sanctions or enforcement actions** — no direct enforcement authority; violations escalate to human review only
- **Business registration manipulation** — no direct write to registration records without human sign-off; intake and verification only

These are not "high-risk operations requiring escalation" — they are entirely outside the actor's design vocabulary. The governor will **permanently :hold** any proposal that touches these categories (it is not a matter of confidence, approval chain, or threshold).

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a document-handling and verification robot performs business registration verification, permit application intake, economic data collection, and complaint escalation under an actor that proposes actions and an independent **Regulation Governor** that gates them. The governor never dispatches a robot action itself; `:high`/`:safety-critical` actions (such as permit application intake above a threshold, or complaint escalation) require human sign-off.

A live sample of the operator console (robotics safety console, shared template) is rendered in [docs/samples/operator-console.html](docs/samples/operator-console.html) — pure-data HTML output of `kotoba.robotics.ui`.

## Core Contract

```text
business registration + permit application + complaint data
        |
        v
Regulation Advisor -> Regulation Governor -> registration verification, permit intake, data logging, or human approval
        |
        v
robot actions (gated) + compliance record + audit ledger
```

No automated advice can dispatch a compliance action the governor refuses, verify a business outside its registered scope, or publish a compliance record without governor approval and audit evidence.

## Capability layer

Resolves via [`kotoba-lang/industry`](https://github.com/kotoba-lang/industry)
(ISIC `8413`). Required capabilities:

- :robotics
- :identity
- :forms
- :dmn
- :bpmn
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## Reference implementation (`:maturity :implemented`)

Full itonami Actor pattern (per ADR-2607011000 / CLAUDE.md's Actors
section): a real
[`kotoba-lang/langgraph`](https://github.com/kotoba-lang/langgraph)
`StateGraph`, with the Advisor and Governor as distinct graph nodes and
human-in-the-loop interrupt/resume via checkpointing.

```text
:intake -> :advise -> :govern -> :decide -+-> :commit            (:ok? true)
                                           +-> :request-approval   (:escalate? true, interrupt-before)
                                           +-> :hold               (:hard? true)
```

- `src/regulation/store.cljc` — `Store` protocol + `MemStore`:
  registered businesses, committed compliance records, an append-only audit ledger.
- `src/regulation/advisor.cljc` — `Advisor` protocol; `mock-advisor`
  (deterministic, default) proposes a regulatory operation from a
  request; `llm-advisor` wraps a `langchain.model/ChatModel` — either
  way the advisor only ever produces a `:propose`-effect proposal,
  never a committed record, and LLM parse failures always yield
  `confidence 0.0` (forces escalation, never fabricated confidence).
- `src/regulation/governor.cljc` — `RegulationGovernor/check`: a pure
  function, wired as its own `:govern` node. Hard invariants
  (unregistered business, a proposal whose `:effect` isn't `:propose`, any
  proposal touching permit grant/denial/tariff setting/binding rulings) always
  route to `:hold`. Escalation invariants (permit application intake, complaint
  escalation, or low advisor confidence) always route to `:request-approval` — an
  `interrupt-before` node that the graph checkpoints and only resumes on explicit
  human approval (`actor/approve!`).
- `src/regulation/actor.cljc` — `build-graph`, `run-request!`,
  `approve!`: the `langgraph.graph/state-graph` wiring itself.

```bash
clojure -M:test
```

This is what backs this repo's `:maturity :implemented` entry in
[`kotoba-lang/industry`](https://github.com/kotoba-lang/industry).

## License

AGPL-3.0-or-later.
