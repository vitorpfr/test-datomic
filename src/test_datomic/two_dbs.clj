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

; check data integrity (are dbs equal?)
(data-integrity/verify db-uri db-uri-replica {:id-attrs [:course/id]})
