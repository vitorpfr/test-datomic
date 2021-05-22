(ns test-datomic.client-api-local-dev
  (:require [datomic.client.api :as d]
            [schema.core :as s]
            [schema-generators.generators :as g]))

; Helper functions
(defn generate-lots-of-courses [conn how-many-batches]
  (dotimes [n how-many-batches]
    (let [courses (g/sample 40 {:course/id s/Uuid})]
      (d/transact conn courses))))

(defn generate-lots-of-students [conn how-many-batches]
  (dotimes [n how-many-batches]
    (let [students (g/sample 40 {:student/id s/Uuid})]
      (d/transact conn students))))


; Creating a database from zero
(def client (d/client {:server-type :dev-local
                       :system      "dev"
                       :storage-dir :mem}))

(d/delete-database client {:db-name "school"})
(d/create-database client {:db-name "school"})
(def conn (d/connect client {:db-name "school"}))

conn

(d/transact conn [{:db/ident       :course/id
                   :db/valueType   :db.type/uuid
                   :db/unique      :db.unique/identity
                   :db/cardinality :db.cardinality/one}
                  {:db/ident       :student/id
                   :db/valueType   :db.type/uuid
                   :db/unique      :db.unique/identity
                   :db/cardinality :db.cardinality/one}])

(generate-lots-of-courses conn 200)
(generate-lots-of-students conn 200)

(d/db-stats (d/db conn))
