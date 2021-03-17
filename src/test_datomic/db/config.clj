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

(defn restart-db! [db-uri]
  (delete-db! db-uri)
  (create-and-open-db! db-uri))

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

; TODO: figure out how to generate mass registrations
(defn generate-mass-data! [conn]
  (dotimes [n 50]
    (let [courses (g/sample 40 m/Course db.gen/leaf-gens)
          students (g/sample 40 m/Student db.gen/leaf-gens)
          semesters (g/sample 40 m/Semester db.gen/leaf-gens)
          data (concat courses students semesters)]
      @(d/transact conn data))))

(defn create-sample-data! [conn]
  (d/transact conn [{:semester/year   2018
                     :semester/season :fall}

                    {:course/id "BIO-101"}
                    {:course/id "CHE-101"}

                    {:student/first "John"
                     :student/last  "Doe"
                     :student/email "johndoe@university.edu"}

                    {:student/first "Mary"
                     :student/last  "Poppins"
                     :student/email "marypoppins@university.edu"}])

  (d/transact conn [{:reg/course [:course/id "BIO-101"]
                     :reg/semester [:semester/year+season [2018 :fall]]
                     :reg/student [:student/email "johndoe@university.edu"]}

                    {:reg/course [:course/id "BIO-101"]
                     :reg/semester [:semester/year+season [2018 :fall]]
                     :reg/student [:student/email "marypoppins@university.edu"]}

                    {:reg/course [:course/id "CHE-101"]
                     :reg/semester [:semester/year+season [2018 :fall]]
                     :reg/student [:student/email "marypoppins@university.edu"]}])
  (println "data loaded into db"))

(defn load-db-with-sample-data!
  [conn]
  (create-schema! conn)
  (create-sample-data! conn))



