(ns test-datomic.replicas.replica-with-excision
  (:require [datomic.api :as d]
            [schema-generators.generators :as g]
            [test-datomic.db.config :as db.config]
            [test-datomic.db.entities :as db.ent]
            [schema.core :as s]))

(def uri "datomic:dev://localhost:4334/school")
(def uri-replica "datomic:dev://localhost:5334/school")

(defn transact-simple-schema [conn]
  (d/transact conn [{:db/ident       :course/id
                     :db/valueType   :db.type/uuid
                     :db/cardinality :db.cardinality/one
                     :db/unique      :db.unique/identity}
                    {:db/ident       :course/name
                     :db/valueType   :db.type/string
                     :db/cardinality :db.cardinality/one}]))


(def conn-replica (db.config/restart-db! uri-replica))
(transact-simple-schema conn-replica)

(def conn (db.config/restart-db! uri))
(transact-simple-schema conn)

(defn generate-data [conn how-many-times]
  (dotimes [n how-many-times]
    (let [courses   (g/sample 2 {:course/id  s/Uuid
                                  :course/name s/Str})]
      @(d/transact conn courses)
      @(d/transact conn [{:db/excise [:course/id (-> courses first :course/id)]}]) ; removes one of the courses
      )))

(generate-data conn 10)

(println "courses entity count on source" (count (db.ent/all-courses (d/db conn))))
(println "courses entity count on target" (count (db.ent/all-courses (d/db conn-replica))))

; count how many excises happened
(d/q '[:find (count ?e) .
       :where [_ :db/excise ?e]]
     (d/db conn))
