(ns test-datomic.playground
  (:use clojure.pprint)
  (:require [datomic.api :as d]
            [schema.core :as s]
            [test-datomic.db.config :as db.config]
            [schema-generators.generators :as g]
            [test-datomic.db.entities :as db.ent]))

(s/set-fn-validation! true)
(def db-uri "datomic:dev://localhost:4334/school-original")
(def db-uri-replica "datomic:dev://localhost:4334/school-ronaldo")

(db.config/delete-db! db-uri)
(db.config/delete-db! db-uri-replica)

(def conn (db.config/create-and-open-db! db-uri))
(def conn-replica (db.config/create-and-open-db! db-uri-replica))
(def conns [conn conn-replica])

; create schema and add data
(run! db.config/create-schema! conns)
;(pmap db.config/generate-mass-data! conns)
(pmap db.config/create-sample-data! conns)

; get db aggregates
(defn get-entities-aggs [db]
  {:count (d/q '[:find (count ?e) .
                 :where [?e :db/ident]]
               db)})

(defn get-transactions-aggs [db]
  (let [tx-times (d/q '[:find [?time ...]
                        :with ?e
                        :where
                        [?instant-entity :db/ident :db/txInstant]
                        [?e ?instant-entity ?time]]
                      db)
        tx-times-ms (map inst-ms tx-times)]
    {:count (count tx-times-ms)
     :max   (apply max tx-times-ms)
     :sum   (reduce + tx-times-ms)}))

(defn get-attributes-aggs [db]
  {:count (d/q '[:find (count ?a) .
                 :where [_ :db.install/attribute ?a]]
               db)})


; final result of a single db
(defn get-aggs [db]
  {:entities     (get-entities-aggs db)
   :transactions (get-transactions-aggs db)
   :attributes   (get-attributes-aggs db)})

; comparison between dbs
(defn equal-dbs? [db-one db-two]
  (let [aggs-one (get-aggs db-one)
        aggs-two (get-aggs db-two)]
    (pprint aggs-one)
    (pprint aggs-two)
    (pprint (= aggs-one aggs-two))))

(->> conns
     (map d/db)
     (apply equal-dbs?))
; resultado é esperado ser falso, pq os bancos não são criados exatamente na mesma hora
; transações deveriam ser idênticas mesmo? replicação não pode criar uma micro-diferença de instante?
; perguntar pro Alex



(db.ent/all-students (d/db conn))
(db.ent/all-semesters (d/db conn))
(db.ent/all-courses (d/db conn))
(db.ent/all-registrations (d/db conn))








;; TESTING: misc
(d/pull (d/db conn) '[*] 5)
; end of testing

;; TESTING: sum all numeric values
; check all existing value types in db
(d/q '[:find ?value-types
       :where
       [?e :db/valueType ?v]
       [?v :db/ident ?value-types]
       ]
     (d/db conn))

(defn get-long-attributes-aggs [db]
  (d/q '[:find ?e ?v
         :where
         [?e :db/valueType :db.type/long]
         [?e _ ?v]]
       db))

(get-long-attributes-aggs (d/db conn))
;; end of testing























