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

;(d/transact conn [{:db/ident       :student/id
;                   :db/valueType   :db.type/uuid
;                   :db/unique      :db.unique/identity
;                   :db/cardinality :db.cardinality/one}])

;(db.config/generate-lots-of-courses conn 20000)
;(db.config/generate-lots-of-students conn 20000)

;(db.ent/all-courses (d/db conn))

(defn test-count-query []
  (println "---- TEST START ----")
  (println "--------------------")
  (println "normal (count ?e)")
  (time (println (db.ent/course-count (d/db conn))))
  (println "--------------------")
  (println "count with qseq and count, :find ?e")
  (time (println (db.ent/course-count-optimized-two (d/db conn))))
  ;(println "--------------------")
  ;(println "count with qseq and count, :find [?e ...]")
  ;(time (println (db.ent/course-count-optimized (d/db conn))))
  (println "--------------------")
  (println "count with simple count of d/datoms on :avet")
  (time (println (db.ent/course-count-optimized-three (d/db conn))))
  (println "--------------------")
  (println "count with d/datoms on :avet and java stuff")
  (time (println (db.ent/course-count-optimized-adv-4 (d/db conn))))
  (println "--------------------")
  (println "count with d/datoms on :avet and eduction")
  (time (println (db.ent/course-count-optimized-adv-1 (d/db conn))))
  ;(println "--------------------")
  ;(println "count with d/datoms on :avet and transduce")
  ;(time (println (db.ent/course-count-optimized-adv-2 (d/db conn))))

  ;(println "--------------------")
  ;(println "count with d/datoms on :aevt and eduction")
  ;(time (println (db.ent/course-count-optimized-adv-3 (d/db conn))))
  )

(test-count-query)

