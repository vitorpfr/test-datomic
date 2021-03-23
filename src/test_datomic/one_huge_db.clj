(ns test-datomic.one-huge-db
  (:use clojure.pprint)
  (:require [datomic.api :as d]
            [schema.core :as s]
            [test-datomic.db.entities :as db.ent]
            [test-datomic.db.config :as db.config]
            [schema-generators.generators :as g]
            [clj-memory-meter.core :as mm]))

(s/set-fn-validation! true)

(def db-uri "datomic:dev://localhost:4334/huge-school")
;(def conn (db.config/restart-db! db-uri))
(def conn (d/connect db-uri))

;(d/transact conn [{:db/ident       :course/id
;                   :db/valueType   :db.type/uuid
;                   :db/unique      :db.unique/identity
;                   :db/cardinality :db.cardinality/one}])

;(db.config/generate-lots-of-entities conn 20000)

;(db.ent/all-courses (d/db conn))

(defn test-count-query []
  (println "---- TEST START ----")
  (println "count with qseq and count, :find ?e")
  (time (println (db.ent/course-count-optimized-two (d/db conn))))
  (println "normal count")
  (time (println (db.ent/course-count (d/db conn))))
  ;(println "count with qseq and count, :find [?e ...]")
  ;(time (println (db.ent/course-count-optimized (d/db conn))))
  (println "count with d/datoms on :avet")
  (time (println (db.ent/course-count-optimized-three (d/db conn))))
  )

(test-count-query)
