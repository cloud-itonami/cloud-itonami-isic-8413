(ns regulation.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [regulation.store :as store]
            [regulation.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-business! st {:business-id "business-1" :name "Acme Corp" :registration-date "2026-01-01" :verified-at "2026-01-15"})
    st))

(deftest ok-on-clean-economic-data-logging
  (let [st (fresh-store)
        proposal {:op :log-economic-statistic :effect :propose :confidence 0.9 :stake :low}
        v (governor/check {:business-id "business-1"} {} proposal st)]
    (is (:ok? v))
    (is (not (:hard? v)))
    (is (not (:escalate? v)))))

(deftest hard-on-unregistered-business
  (let [st (fresh-store)
        proposal {:op :verify-business-registration :effect :propose :confidence 0.9 :stake :low}
        v (governor/check {:business-id "no-such-business"} {} proposal st)]
    (is (:hard? v))
    (is (some #(= :no-business (:rule %)) (:violations v)))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        proposal {:op :verify-business-registration :effect :direct-write :confidence 0.9 :stake :low}
        v (governor/check {:business-id "business-1"} {} proposal st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

(deftest hard-on-scope-boundary-violation
  (let [st (fresh-store)
        proposal {:op :unknown :effect :propose :confidence 0.0 :stake :high}
        v (governor/check {:business-id "business-1"} {} proposal st)]
    (is (:hard? v))
    (is (some #(= :scope-boundary (:rule %)) (:violations v)))))

(deftest escalates-on-permit-application-intake
  (let [st (fresh-store)
        proposal {:op :intake-permit-application :effect :propose :confidence 0.9 :stake :medium}
        v (governor/check {:business-id "business-1"} {} proposal st)]
    (is (:escalate? v))
    (is (not (:hard? v)))))

(deftest escalates-on-complaint-flag
  (let [st (fresh-store)
        proposal {:op :flag-complaint :effect :propose :confidence 0.9 :stake :high}
        v (governor/check {:business-id "business-1"} {} proposal st)]
    (is (:escalate? v))
    (is (not (:hard? v)))))

(deftest escalates-on-low-confidence
  (let [st (fresh-store)
        proposal {:op :verify-business-registration :effect :propose :confidence 0.2 :stake :low}
        v (governor/check {:business-id "business-1"} {} proposal st)]
    (is (:escalate? v))
    (is (not (:hard? v)))))

(deftest store-records-and-ledger-append-only
  (let [st (fresh-store)]
    (store/commit-record! st {:business-id "business-1" :op :log-economic-statistic})
    (store/append-ledger! st {:disposition :commit})
    (is (= 1 (count (store/records-of st "business-1"))))
    (is (= 1 (count (store/ledger st))))))
