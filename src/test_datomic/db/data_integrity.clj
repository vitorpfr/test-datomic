(ns test-datomic.db.data-integrity
  (:require [clojure.data :as data]
            [datomic.api :as d]
            [test-datomic.db.util :as util]))

;(defn ^:private get-entity-count
;  [db db-order id-attr]
;  (let [entity-count (reduce + 0 (eduction (map (constantly 1)) (d/datoms db :aevt id-attr)))]
;    {:count {id-attr entity-count}}))
;
;(defn ^:private get-unique-id-attrs [db db-order]
;  (d/q '[:find [?v ...]
;         :where
;         [?e :db/unique :db.unique/identity]
;         [?e :db/ident ?v]]
;       db))
;
;(defn ^:private get-entities-aggs [db db-order id-attrs]
;  (let [coll     (or id-attrs (get-unique-id-attrs db db-order))
;        count-fn (partial get-entity-count db db-order)]
;    (->> (pmap count-fn coll)
;         doall
;         (apply merge-with merge))))
;
;(defn ^:private get-transactions-count [db db-order]
;  (reduce + 0 (eduction (map (constantly 1)) (d/datoms db :aevt :db/txInstant))))
;
;(defn ^:private get-transactions-max [db db-order tx-max]
;  (d/q '[:find ?v .
;         :in $ ?tx
;         :where [?tx :db/txInstant ?v]] db tx-max))
;
;(defn ^:private get-transactions-aggs [db db-order]
;  (let [tx-count (get-transactions-count db db-order)
;        tx-max (-> db
;                   d/basis-t
;                   d/t->tx)
;        tx-max-inst (get-transactions-max db db-order tx-max)]
;    {:count tx-count
;     :max   (inst-ms tx-max-inst)}))
;
;(defn ^:private get-attributes-aggs [db db-order]
;  {:count (reduce + 0 (eduction (map (constantly 1)) (d/datoms db :aevt :db.install/attribute)))})
;
;(defn ^:private get-db-aggs [db db-order id-attrs]
;  (let [values (pvalues (get-entities-aggs db db-order id-attrs)
;                        (get-transactions-aggs db db-order)
;                        (get-attributes-aggs db db-order))]
;    (zipmap [:entities :transactions :attributes] values)))
;
;(defn ^:private dbs-differences
;  [db-one-metrics db-two-metrics]
;  (let [[data-only-in-db-one data-only-in-db-two data-in-both] (data/diff db-one-metrics db-two-metrics)]
;    {:first-db-only  data-only-in-db-one
;     :second-db-only data-only-in-db-two
;     :equal          data-in-both}))
;
;(defn ^:private db-metrics [db-uri id-attrs db-order]
;  (-> (d/connect db-uri)
;      d/db
;      (get-db-aggs db-order id-attrs)))
;
;(defn equivalent-dbs?
;  "Checks if two different dbs can be considered equivalent (same aggregations of entities, attributes and transactions)"
;  [first-db-uri second-db-uri id-attrs]
;  (let [values (pvalues (db-metrics first-db-uri id-attrs :first)
;                        (db-metrics second-db-uri id-attrs :second))
;        [first-db-metrics second-db-metrics] (doall values)
;        base {:first-db-uri  first-db-uri
;              :second-db-uri second-db-uri}]
;    (merge base (if (= first-db-metrics second-db-metrics)
;                  {:equivalent-dbs? true
;                   :data            first-db-metrics}
;                  {:equivalent-dbs? false
;                   :data            (dbs-differences first-db-metrics second-db-metrics)}))))

(def ^:private sync-excise-timeout-ms 30000)

(defn ^:private get-datoms-count
  [db index attribute]
  (reduce + 0 (eduction (map (constantly 1)) (d/datoms db index attribute))))

(defn ^:private get-entity-count
  [db db-order id-attr]
  (util/do-returning-first
    {:count {id-attr (get-datoms-count db :avet id-attr)}}
    (println :db db-order :op :get-entity-count :id-attribute id-attr)))

(defn ^:private get-unique-id-attrs [db db-order]
  (util/do-returning-first
    (d/q '[:find [?v ...]
           :where
           [?e :db/unique :db.unique/identity]
           [?e :db/ident ?v]]
         db)
    (println :db db-order :op :get-unique-id-attrs)))

(defn ^:private get-entities-aggs [db db-order id-attrs]
  (let [count-fn (partial get-entity-count db db-order)]
    (->> (pmap count-fn id-attrs)
         doall
         (do (println :db db-order :op :get-entities-aggs))
         (apply merge-with merge))))

(defn ^:private get-transactions-count [db db-order]
  (util/do-returning-first
    (get-datoms-count db :aevt :db/txInstant)
    (println :db db-order :op :get-transactions-count)))

(defn ^:private get-transactions-max [db db-order]
  (util/do-returning-first
    (-> (d/datoms db :aevt :db/txInstant)
        last
        :v
        inst-ms)
    (println :db db-order :op :get-transactions-max)))

(defn ^:private get-transactions-aggs [db db-order]
  (util/do-returning-first
    {:count (get-transactions-count db db-order)
     :max   (get-transactions-max db db-order)}
    (println :db db-order :op :get-transactions-aggs)))

(defn ^:private get-attributes-aggs [db db-order]
  (util/do-returning-first
    {:count (get-datoms-count db :aevt :db.install/attribute)}
    (println :db db-order :op :get-attributes-aggs)))

(defn ^:private get-db-aggs [db db-order id-attrs]
  (println (str "------- START OF " (.toUpperCase (name db-order)) " DB METRICS CALCULATION -------"))
  (let [values (pvalues (get-entities-aggs db db-order id-attrs)
                        (get-transactions-aggs db db-order)
                        (get-attributes-aggs db db-order))]
    (println :db db-order :op :get-db-aggs)
    (zipmap [:entities :transactions :attributes] values)))

(defn ^:private dbs-differences
  [db-one-metrics db-two-metrics]
  (let [[data-only-in-db-one data-only-in-db-two data-in-both] (data/diff db-one-metrics db-two-metrics)]
    {:first-db-only  data-only-in-db-one
     :second-db-only data-only-in-db-two
     :equal          data-in-both}))

(defn ^:private sync-excise
  [db-uri]
  (let [conn      (d/connect db-uri)
        db        (d/db conn)
        basis-t   (d/basis-t db)
        db-synced (deref (d/sync-excise conn basis-t) sync-excise-timeout-ms nil)]
    (if db-synced
      db-synced
      (do
        db))))

(defn ^:private db-metrics [db-uri id-attrs db-order start end]
  (let [db          (sync-excise db-uri)
        filtered-db (cond (and start end)       (-> db (d/since start) (d/as-of end))
                          (and start (not end)) (d/since db start)
                          (and (not start) end) (d/as-of db end)
                          :else                 db)]
    (get-db-aggs filtered-db db-order (or id-attrs (get-unique-id-attrs db db-order)))))

(defn ^:private equivalent-dbs?
  "Checks if two different dbs can be considered equivalent (same aggregations of entities, attributes and transactions)"
  [first-db-uri second-db-uri id-attrs start end]
  (let [values (pvalues (db-metrics first-db-uri id-attrs :first start end)
                        (db-metrics second-db-uri id-attrs :second start end))
        [first-db-metrics second-db-metrics] (doall values)
        base {:first-db-uri  first-db-uri
              :second-db-uri second-db-uri}]
    (merge base (if (= first-db-metrics second-db-metrics)
                  {:equivalent-dbs? true
                   :data            first-db-metrics}
                  {:equivalent-dbs? false
                   :data            (dbs-differences first-db-metrics second-db-metrics)}))))

(defn ^:private elapsed-time [start-time-seconds finish-time-seconds]
  (let [diff (- finish-time-seconds start-time-seconds)
        seconds-in-a-minute 60]
    (str (quot diff seconds-in-a-minute) "m " (rem diff seconds-in-a-minute) "s")))

(defn data-integrity-check
  "Performs a data integrity check between two dbs, checking if they have equivalent data and reporting differences (if any).
  Optional args are allowed:
  - :id-attrs (e.g. [:customer/id :prospect/id]): A list of datomic attributes. If provided, the check will only count those attributes. If not provided, the check will count all db attributes with :db/unique set to :db.unique/identity.
  - :start (e.g. 2019-03-10T00:00:00.000Z): If a start datetime is provided, the check will be performed starting from this point. It IS NOT inclusive (due to d/since behavior), which means that transactions that happened on this datetime itself will not be considered.
  - :end (e.g. 2019-03-12T00:00:00.000Z): If a end datetime is provided, the check will be performed until this point. It IS inclusive (due to d/as-of behavior), which means that transactions that happened on this datetime itself will be considered."
  [first-db-uri second-db-uri {:keys [id-attrs start end]}]
  (let [start-time (util/now-secs)
        result (equivalent-dbs? first-db-uri second-db-uri id-attrs start end)]
    (println "------- DATA INTEGRITY CHECK RESULTS -------")
    (println :result result :elapsed-time (elapsed-time start-time (util/now-secs)))
    result))

