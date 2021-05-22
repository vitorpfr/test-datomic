(ns test-datomic.queries-history
  (:use clojure.pprint)
  (:require [datomic.api :as d]
            [schema.core :as s]
            [test-datomic.db.entities :as db.ent]
            [test-datomic.db.config :as db.config]
            [schema-generators.generators :as g]))

(def db-uri "datomic:dev://localhost:4334/school")
(def conn (db.config/restart-db! db-uri))

(def schema [{:db/ident       :student/first
              :db/valueType   :db.type/string
              :db/cardinality :db.cardinality/one}
             {:db/ident       :student/last
              :db/valueType   :db.type/string
              :db/cardinality :db.cardinality/one}
             {:db/ident       :student/email
              :db/valueType   :db.type/string
              :db/cardinality :db.cardinality/one
              :db/unique      :db.unique/identity}

             {:db/ident       :course/id
              :db/valueType   :db.type/string
              :db/unique      :db.unique/identity
              :db/cardinality :db.cardinality/one}
             {:db/ident       :course/name
              :db/valueType   :db.type/string
              :db/cardinality :db.cardinality/one}

             {:db/ident       :reg/id
              :db/valueType   :db.type/long
              :db/cardinality :db.cardinality/one
              :db/unique      :db.unique/identity}
             {:db/ident       :reg/course
              :db/valueType   :db.type/ref
              :db/cardinality :db.cardinality/one}
             {:db/ident       :reg/student
              :db/valueType   :db.type/ref
              :db/cardinality :db.cardinality/one}
             ])
@(d/transact conn schema)

(def data [
           {:course/id "BIO-101"}
           {:course/id "CHE-101"}

           {:student/first "John"
            :student/last  "Doe"
            :student/email "johndoe@university.edu"}
           {:student/first "Mary"
            :student/last  "Poppins"
            :student/email "marypoppins@university.edu"}
           ])
@(d/transact conn data)

(def more-data [{:reg/id      1
                 :reg/course  [:course/id "BIO-101"]
                 :reg/student [:student/email "johndoe@university.edu"]}])
@(d/transact conn more-data)

(db.ent/all-students (d/db conn))
(db.ent/all-courses (d/db conn))

(def db (d/db conn))

(d/q '[:find (pull $ ?e [*])
       :in $ $his
       :where [$his ?e :reg/id]]
     db
     (d/history db))

