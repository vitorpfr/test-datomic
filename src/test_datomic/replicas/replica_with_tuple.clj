(ns test-datomic.replicas.replica-with-tuple
  (:require [datomic.api :as d]
            [schema-generators.generators :as g]
            [test-datomic.db.config :as db.config]
            [test-datomic.db.entities :as db.ent]
            [schema.core :as s]))

(def uri "datomic:dev://localhost:4334/school-original")
(def uri-replica "datomic:dev://localhost:4334/school-replica")

(defn transact-simple-schema [conn]
  (d/transact conn [{:db/ident       :semester/year
                     :db/valueType   :db.type/uuid
                     :db/cardinality :db.cardinality/one}
                    {:db/ident       :semester/season
                     :db/valueType   :db.type/uuid
                     :db/cardinality :db.cardinality/one}
                    {:db/ident       :semester/year+season
                     :db/valueType   :db.type/tuple
                     :db/tupleAttrs  [:semester/year :semester/season]
                     :db/cardinality :db.cardinality/one
                     :db/unique      :db.unique/identity}]))


(def conn-replica (db.config/restart-db! uri-replica))
(transact-simple-schema conn-replica)

(def conn (db.config/restart-db! uri))
(transact-simple-schema conn)

(defn generate-semester [conn how-many-times]
  (dotimes [n how-many-times]
    (let [courses (g/sample 1 {:semester/year   s/Uuid
                               :semester/season s/Uuid})]
      @(d/transact conn courses))))

(generate-semester conn 6000)

(println "semesters entity count on source" (count (db.ent/all-semesters (d/db conn))))
(println "courses entity count on target" (count (db.ent/all-semesters (d/db conn-replica))))

(db.ent/all-semesters (d/db conn))
(db.ent/all-semesters (d/db conn-replica))

; basis-t of original and replica are still equal, however, datomic cannot guarantee that
