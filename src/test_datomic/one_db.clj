(ns test-datomic.one-db
  (:use clojure.pprint)
  (:require [datomic.api :as d]
            [schema.core :as s]
            [test-datomic.db.entities :as db.ent]
            [test-datomic.db.config :as db.config]
            [schema-generators.generators :as g]))

(s/set-fn-validation! true)

(def db-uri "datomic:dev://localhost:4334/school")
(def conn (db.config/restart-db! db-uri))
(db.config/create-schema! conn)
(db.config/load-db-with-sample-data! conn)

(db.ent/all-students (d/db conn))
(db.ent/all-semesters (d/db conn))
(db.ent/all-courses (d/db conn))
(db.ent/all-registrations (d/db conn))
