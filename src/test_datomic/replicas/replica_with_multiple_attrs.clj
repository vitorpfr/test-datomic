(ns test-datomic.replicas.replica-with-multiple-attrs
  (:require [datomic.api :as d]
            [schema-generators.generators :as g]
            [test-datomic.db.config :as db.config]
            [test-datomic.db.entities :as db.ent]
            [schema.core :as s]))

(def uri "datomic:dev://localhost:4334/school")
(def uri-replica "datomic:dev://localhost:5334/school")

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
                     :db/unique      :db.unique/identity}

                    {:db/ident       :course/id
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
    (let [semesters (g/sample 5 {:semester/year   s/Uuid
                                 :semester/season s/Uuid})
          courses   (g/sample 32 {:course/id  s/Uuid
                                  :course/name s/Str})]
      @(d/transact conn semesters)
      @(d/transact conn courses)
      @(d/transact conn [[:db/add [:course/id (-> courses first :course/id)] :course/name "new-name"]]) ; substitutes the :course/name
      )))

(generate-data conn 300)

(println "semesters entity count on source" (count (db.ent/all-semesters (d/db conn))))
(println "semesters entity count on target" (count (db.ent/all-semesters (d/db conn-replica))))
(println "courses entity count on source" (count (db.ent/all-courses (d/db conn))))
(println "courses entity count on target" (count (db.ent/all-courses (d/db conn-replica))))

; basis-t of original and replica are still equal, however, datomic cannot guarantee that
