(ns regulation.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [regulation.store :as store]
            [regulation.advisor :as advisor]
            [regulation.actor :as actor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-business! st {:business-id "business-1" :name "Acme Corp" :registration-date "2026-01-01" :verified-at "2026-01-15"})
    st))

(deftest builds-graph-successfully
  (let [st (fresh-store)
        graph (actor/build-graph {:store st :advisor (advisor/mock-advisor)})]
    (is (some? graph))))

(deftest run-request-completes-on-clean-operation
  (let [st (fresh-store)
        graph (actor/build-graph {:store st :advisor (advisor/mock-advisor)})
        request {:business-id "business-1"}
        result (actor/run-request! graph request {} "thread-1")]
    (is (some? result))
    (is (contains? result :state))
    (is (contains? result :status))))

(deftest holds-on-unregistered-business
  (let [st (fresh-store)
        graph (actor/build-graph {:store st :advisor (advisor/mock-advisor)})
        request {:business-id "no-such-business"}
        result (actor/run-request! graph request {} "thread-2")]
    (is (some? result))
    (is (= :done (:status result)))))
