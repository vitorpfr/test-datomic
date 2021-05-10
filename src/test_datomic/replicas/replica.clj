(ns test-datomic.replicas.replica
  (:require [datomic.api :as d]
            [test-datomic.db.config :as db.config]
            [test-datomic.db.entities :as db.ent]))

(def uri "datomic:dev://localhost:4334/school-original")
(def uri-replica "datomic:dev://localhost:4334/school-replica")

(defn transact-simple-schema [conn]
  (d/transact conn [{:db/ident       :course/id
                     :db/valueType   :db.type/uuid
                     :db/unique      :db.unique/identity
                     :db/cardinality :db.cardinality/one}]))


(def conn-replica (db.config/restart-db! uri-replica))
(transact-simple-schema conn-replica)

(def conn (db.config/restart-db! uri))
(transact-simple-schema conn)
(db.config/generate-single-course conn 3000)
;(db.config/generate-lots-of-courses conn 1300)

(println "courses entity count on source" (count (db.ent/all-courses (d/db conn))))
(println "courses entity count on target" (count (db.ent/all-courses (d/db conn-replica))))

(db.ent/all-courses (d/db conn))
(db.ent/all-courses (d/db conn-replica))

; basis-t of original and replica are still equal, however, datomic cannot guarantee that
