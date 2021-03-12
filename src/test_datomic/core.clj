(ns test-datomic.core
  (:use clojure.pprint)
  (:require [datomic.api :as d]
            [schema.core :as s]
            [test-datomic.db.entities :as db.ent]
            [test-datomic.db.config :as db.config]
            [schema-generators.generators :as g]))

(s/set-fn-validation! true)

(def db-uri "datomic:dev://localhost:4334/school")

(db.config/delete-db! db-uri)
(def conn (db.config/create-and-open-db! db-uri))
(db.config/create-schema! conn)
(db.config/create-data! conn)

(println "db loaded")

(db.ent/all-students (d/db conn))
(db.ent/all-semesters (d/db conn))
(db.ent/all-courses (d/db conn))
(db.ent/all-registrations (d/db conn))

; explorando dados dentro do banco

; entidade 17592186045420 tem atributos 72, 73 e 74
(d/q '[:find ?attr
       :where
       [17592186045420 ?attr]]
     (d/db conn))

(d/q '[:find ?attr-name
       :where
       [17592186045420 ?attr]
       [?attr ?attr-name]]
     (d/db conn))


; futuro: gerar vários students, semesters e courses
;(defn gera-10000-produtos [conn]
;  (dotimes [n 50]
;    (let [produtos-gerados (g/sample 200 model/Produto generators/leaf-generators)]
;      (println n (count @(db.produto/adiciona-ou-altera! conn produtos-gerados))))))
