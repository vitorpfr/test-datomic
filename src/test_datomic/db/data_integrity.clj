(ns test-datomic.db.data-integrity
  (:require [clojure.data :as data]
            [datomic.api :as d]))

(defn ^:private get-entity-count
  [db db-order id-attr]
  (let [entity-count (reduce + 0 (eduction (map (constantly 1)) (d/datoms db :aevt id-attr)))]
    {:count {id-attr entity-count}}))

(defn ^:private get-unique-id-attrs [db db-order]
  (d/q '[:find [?v ...]
         :where
         [?e :db/unique :db.unique/identity]
         [?e :db/ident ?v]]
       db))

(defn ^:private get-entities-aggs [db db-order id-attrs]
  (let [coll     (or id-attrs (get-unique-id-attrs db db-order))
        count-fn (partial get-entity-count db db-order)]
    (->> (pmap count-fn coll)
         doall
         (apply merge-with merge))))

(defn ^:private get-transactions-count [db db-order]
  (reduce + 0 (eduction (map (constantly 1)) (d/datoms db :aevt :db/txInstant))))

(defn ^:private get-transactions-max [db db-order tx-max]
  (d/q '[:find ?v .
         :in $ ?tx
         :where [?tx :db/txInstant ?v]] db tx-max))

(defn ^:private get-transactions-aggs [db db-order]
  (let [tx-count (get-transactions-count db db-order)
        tx-max (-> db
                   d/basis-t
                   d/t->tx)
        tx-max-inst (get-transactions-max db db-order tx-max)]
    {:count tx-count
     :max   (inst-ms tx-max-inst)}))

(defn ^:private get-attributes-aggs [db db-order]
  {:count (reduce + 0 (eduction (map (constantly 1)) (d/datoms db :aevt :db.install/attribute)))})

(defn ^:private get-db-aggs [db db-order id-attrs]
  (let [values (pvalues (get-entities-aggs db db-order id-attrs)
                        (get-transactions-aggs db db-order)
                        (get-attributes-aggs db db-order))]
    (zipmap [:entities :transactions :attributes] values)))

(defn ^:private dbs-differences
  [db-one-metrics db-two-metrics]
  (let [[data-only-in-db-one data-only-in-db-two data-in-both] (data/diff db-one-metrics db-two-metrics)]
    {:first-db-only  data-only-in-db-one
     :second-db-only data-only-in-db-two
     :equal          data-in-both}))

(defn ^:private db-metrics [db-uri id-attrs db-order]
  (-> (d/connect db-uri)
      d/db
      (get-db-aggs db-order id-attrs)))

(defn equivalent-dbs?
  "Checks if two different dbs can be considered equivalent (same aggregations of entities, attributes and transactions)"
  [first-db-uri second-db-uri id-attrs]
  (let [values (pvalues (db-metrics first-db-uri id-attrs :first)
                        (db-metrics second-db-uri id-attrs :second))
        [first-db-metrics second-db-metrics] (doall values)
        base {:first-db-uri  first-db-uri
              :second-db-uri second-db-uri}]
    (merge base (if (= first-db-metrics second-db-metrics)
                  {:equivalent-dbs? true
                   :data            first-db-metrics}
                  {:equivalent-dbs? false
                   :data            (dbs-differences first-db-metrics second-db-metrics)}))))
