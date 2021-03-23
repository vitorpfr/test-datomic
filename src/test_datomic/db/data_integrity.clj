(ns test-datomic.db.data-integrity
  (:require [clojure.data :as data]
            [datomic.api :as d]))

(defn ^:private assoc-entities-count
  [db result id-attr]
  (time (assoc-in result [:count id-attr] (d/q '[:find (count ?e) .
                                                 :in $ ?a
                                                 :where [?e ?a]] db id-attr))))

(defn ^:private assoc-entities-count-optimized
  [db result id-attr]
  (time (assoc-in result [:count id-attr] (ffirst (d/qseq {:query '[:find (count ?e)
                                                                    :in $ ?a
                                                                    :where [?e ?a]]
                                                           :args  [db id-attr]})))))

(defn get-unique-id-attrs [db]
  (d/q '[:find [?v ...]
         :where
         [?e :db/unique :db.unique/identity]
         [?e :db/ident ?v]]
       db))

(defn ^:private get-entities-aggs [db]
  (reduce (partial assoc-entities-count-optimized db) {} (get-unique-id-attrs db)))

(defn ^:private get-transactions-aggs [db]
  (let [tx-count (d/q '[:find (count ?tx) .
                        :where [?tx :db/txInstant]] db)
        tx-max (-> db
                   d/basis-t
                   d/t->tx)
        tx-max-inst (d/q '[:find ?v .
                           :in $ ?tx
                           :where [?tx :db/txInstant ?v]] db tx-max)]
    {:count tx-count
     :max   (inst-ms tx-max-inst)}))

(defn ^:private get-attributes-aggs [db]
  {:count (d/q '[:find (count ?a) .
                 :where [_ :db.install/attribute ?a]] db)})

(defn ^:private get-db-aggs [db]
  {:entities     (get-entities-aggs db)
   :transactions (get-transactions-aggs db)
   :attributes   (get-attributes-aggs db)})

(defn ^:private dbs-differences
  [db-one-uri db-one-metrics db-two-uri db-two-metrics]
  (let [diff (data/diff db-one-metrics db-two-metrics)]
    {db-one-uri (first diff)
     db-two-uri (second diff)}))

(defn ^:private db-metrics [db-uri]
  (-> (d/connect db-uri)
      d/db
      get-db-aggs))

(defn verify
  "Verifies if two different dbs can be considered equivalent (same aggregations of entities, attributes and transactions)"
  [source-db-uri target-db-uri]
  (let [source-db-metrics (db-metrics source-db-uri)
        target-db-metrics (db-metrics target-db-uri)]
    (if (= source-db-metrics target-db-metrics)
      {:result :ok}
      {:result :nok
       :diff   (dbs-differences source-db-uri source-db-metrics target-db-uri target-db-metrics)})))

;; TESTING: misc


; end of testing
