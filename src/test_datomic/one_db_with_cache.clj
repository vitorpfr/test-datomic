(ns test-datomic.one-db-with-cache
  (:use clojure.pprint)
  (:require [datomic.api :as d]
            [schema.core :as s]
            [test-datomic.db.entities :as db.ent]
            [test-datomic.db.config :as db.config]
            [test-datomic.cache.config :as cache]))

(s/set-fn-validation! true)

(def db-uri "datomic:dev://localhost:4334/school-cached")
(def conn (db.config/restart-db! db-uri))
(db.config/create-schema! conn)
(db.config/load-db-with-sample-data! conn)

(db.ent/all-students (d/db conn))
(db.ent/all-semesters (d/db conn))
(db.ent/all-courses (d/db conn))
(db.ent/all-registrations (d/db conn))

(cache/clear)

; first attempt to consult cache data before datomic data:
(defn get-course-on-datomic [db id]
  (println "course retrieved from datomic")
  (d/q '[:find [(pull ?e [*])]
         :in $ ?id
         :where [?e :course/id ?id]]
       db id))

(defn get-course-on-cache [id]
  (println "course retrieved from cache")
  (cache/get id))

(defn get-course [db id]
  (if-let [cache-data (get-course-on-cache id)]
    cache-data
    (when-let [datomic-data (get-course-on-datomic db id)]
      (cache/set id datomic-data)                     ; bug: cache data should have a TTL
      datomic-data)))

; on the first run it gets from datomic
; from the second run onwards, it gets the value from the cache
(get-course (d/db conn) "BIO-101")
