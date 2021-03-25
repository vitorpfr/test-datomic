(ns test-datomic.two-dbs
  (:use clojure.pprint)
  (:require [datomic.api :as d]
            [schema.core :as s]
            [test-datomic.db.config :as db.config]
            [schema-generators.generators :as g]
            [test-datomic.db.entities :as db.ent]
            [test-datomic.db.data-integrity :as data-integrity]))

(s/set-fn-validation! true)
; set uris
(def db-uri "datomic:dev://localhost:4334/school-original")
(def db-uri-replica "datomic:dev://localhost:4334/school-ronaldo")
(def uris [db-uri db-uri-replica])

; restart and load dbs
(def conns (mapv db.config/restart-db! uris))
(run! db.config/load-db-with-sample-data! conns)

;(run! db.config/generate-mass-data! conns) ; not working

; check data on one of the tables
(def conn (first conns))
(db.ent/all-students (d/db conn))
(db.ent/all-semesters (d/db conn))
(db.ent/all-courses (d/db conn))
(db.ent/all-registrations (d/db conn))

; adding an additional entity to the origin db
(d/transact conn [{:course/id "CHE-105"}])
(d/transact conn [{:student/email "a@b.c"
                   :student/first "ola"}])

; check data integrity (are dbs equal?)
(data-integrity/equivalent-dbs? db-uri db-uri-replica nil)

;(defn get-unique-id-attrs [db]
;  (d/q '[:find [?v ...]
;         :where
;         [?e :db/unique :db.unique/identity]
;         [?e :db/ident ?v]]
;       db))
;
;(get-unique-id-attrs (d/db conn))
;
;(test-datomic.db.entities/one (d/db conn) 10)
;
;(reduce + 0 (eduction (map (constantly 1)) (d/datoms (d/db conn) :aevt :db.install/attribute)))
;(reduce + 0 (eduction (map (constantly 1)) (d/datoms (d/db conn) :aevt :db/txInstant)))
;
;(d/q '[:find (count ?a) .
;       :where [_ :db.install/attribute ?a]] (d/db conn))
;
;(count (d/qseq {:query '[:find ?tx
;                         :where [?tx :db/txInstant]]
;                :args  [(d/db conn)]}))
