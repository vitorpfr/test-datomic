(ns test-datomic.db.config
  (:use clojure.pprint)
  (:require [datomic.api :as d]
            [schema-generators.generators :as g]
            [test-datomic.db.generators :as db.gen]
            [test-datomic.model :as m]))

(def db-uri "datomic:dev://localhost:4334/school")

(defn create-and-open-db! [db-uri]
  (println (str "db with uri " db-uri " started"))
  (d/create-database db-uri)
  (d/connect db-uri))

(defn delete-db! [db-uri]
  (println (str "db with uri " db-uri " deleted"))
  (d/delete-database db-uri))


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

             {:db/ident       :semester/year
              :db/valueType   :db.type/long
              :db/cardinality :db.cardinality/one}
             {:db/ident       :semester/season
              :db/valueType   :db.type/keyword
              :db/cardinality :db.cardinality/one}
             {:db/ident       :semester/year+season
              :db/valueType   :db.type/tuple
              :db/tupleAttrs  [:semester/year :semester/season]
              :db/cardinality :db.cardinality/one
              :db/unique      :db.unique/identity}

             {:db/ident       :course/id
              :db/valueType   :db.type/string
              :db/unique      :db.unique/identity
              :db/cardinality :db.cardinality/one}
             {:db/ident       :course/name
              :db/valueType   :db.type/string
              :db/cardinality :db.cardinality/one}

             {:db/ident       :reg/course
              :db/valueType   :db.type/ref
              :db/cardinality :db.cardinality/one}
             {:db/ident       :reg/semester
              :db/valueType   :db.type/ref
              :db/cardinality :db.cardinality/one}
             {:db/ident       :reg/student
              :db/valueType   :db.type/ref
              :db/cardinality :db.cardinality/one}

             {:db/ident       :reg/semester+course+student
              :db/valueType   :db.type/tuple
              :db/tupleAttrs  [:reg/course :reg/semester :reg/student]
              :db/cardinality :db.cardinality/one
              :db/unique      :db.unique/identity}
             ])

(defn create-schema! [conn]
  (d/transact conn schema))

; problema: se eu transacionar mais de 1 sample, ele gera samples com id igual pq é string
; solução: mudar id string pra uuid
(defn generate-mass-data! [conn]
  (dotimes [n 5]
    (let [courses (g/sample 40 m/Course db.gen/leaf-gens)
          students (g/sample 40 m/Student db.gen/leaf-gens)
          data (concat courses students)]
      (println data)
      (d/transact conn data)
      ;(pprint @(d/transact conn (concat courses students)))
      )))

(g/sample 20 m/Course db.gen/leaf-gens)

(defn create-sample-data! [conn]
  (d/transact conn [{:semester/year   2018
                     :semester/season :fall}

                    {:course/id "BIO-101"}

                    {:student/first "John"
                     :student/last  "Doe"
                     :student/email "johndoe@university.edu"}])

  (d/transact conn [{:reg/course [:course/id "BIO-101"]
                     :reg/semester [:semester/year+season [2018 :fall]]
                     :reg/student [:student/email "johndoe@university.edu"]}])
  (println "data loaded into db"))




