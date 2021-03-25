(ns test-datomic.db.data-integrity
  (:require [clojure.data :as data]
            [datomic.api :as d]))

(defn ^:private get-entity-count
  [db db-uri result id-attr]
  (let [entity-count (reduce + 0 (eduction (map (constantly 1)) (d/datoms db :avet id-attr)))]
    (assoc-in result [:count id-attr] entity-count)))

(defn ^:private get-unique-id-attrs [db db-uri]
  (d/q '[:find [?v ...]
         :where
         [?e :db/unique :db.unique/identity]
         [?e :db/ident ?v]]
       db))

(defn ^:private get-entities-aggs [db db-uri id-attrs]
  (reduce (partial get-entity-count db db-uri) {} (or id-attrs (get-unique-id-attrs db db-uri))))

(defn ^:private get-transactions-count [db db-uri]
  (reduce + 0 (eduction (map (constantly 1)) (d/datoms db :aevt :db/txInstant))))

(defn ^:private get-transactions-max [db db-uri tx-max]
  (d/q '[:find ?v .
         :in $ ?tx
         :where [?tx :db/txInstant ?v]] db tx-max))

(defn ^:private get-transactions-aggs [db db-uri]
  (let [tx-count (get-transactions-count db db-uri)
        tx-max (-> db
                   d/basis-t
                   d/t->tx)
        tx-max-inst (get-transactions-max db db-uri tx-max)]
    {:count tx-count
     :max   (inst-ms tx-max-inst)}))

(defn ^:private get-attributes-aggs [db db-uri]
  {:count (reduce + 0 (eduction (map (constantly 1)) (d/datoms db :aevt :db.install/attribute)))})

(defn ^:private get-db-aggs [db db-uri id-attrs]
  {:entities     (get-entities-aggs db db-uri id-attrs)
   :transactions (get-transactions-aggs db db-uri)
   :attributes   (get-attributes-aggs db db-uri)})

(defn ^:private dbs-differences
  [db-one-uri db-one-metrics db-two-uri db-two-metrics]
  (let [[data-only-in-db-one data-only-in-db-two data-in-both] (data/diff db-one-metrics db-two-metrics)]
    {:diff {db-one-uri data-only-in-db-one
            db-two-uri data-only-in-db-two}
     :equal data-in-both}))

(defn ^:private db-metrics [db-uri id-attrs]
  (-> (d/connect db-uri)
      d/db
      (get-db-aggs db-uri id-attrs)))

(defn equivalent-dbs?
  "Checks if two different dbs can be considered equivalent (same aggregations of entities, attributes and transactions)"
  [first-db-uri second-db-uri id-attrs]
  (let [first-db-metrics (db-metrics first-db-uri id-attrs)
        second-db-metrics (db-metrics second-db-uri id-attrs)]
    (if (= first-db-metrics second-db-metrics)
      {:equivalent-dbs? true
       :data            first-db-metrics}
      {:equivalent-dbs? false
       :data            (dbs-differences first-db-uri first-db-metrics second-db-uri second-db-metrics)})))

;; TESTING: misc


; end of testing
