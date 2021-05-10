(ns test-datomic.asof-and-since-db
  (:use clojure.pprint)
  (:require [datomic.api :as d]
            [schema.core :as s]
            [test-datomic.db.entities :as db.ent]
            [test-datomic.db.config :as db.config]
            [schema-generators.generators :as g])
  (:import (java.time LocalDateTime ZoneId)))

(s/set-fn-validation! true)

(def db-uri "datomic:dev://localhost:4334/school")
(def conn (db.config/restart-db! db-uri))

(def db-initial-state (d/db conn))

; load data with an interval, register the time in the middle of the interval
(db.config/load-db-with-sample-data! conn)
(println "time before sleep" (java.util.Date.))
(Thread/sleep 5000)
(def t1 (java.util.Date.))
(Thread/sleep 2500)
@(d/transact conn [{:course/id "MAT-101"}])
(Thread/sleep 2500)
(def t2 (java.util.Date.))
(Thread/sleep 5000)
@(d/transact conn [{:course/id "FIS-101"}
                  {:student/email "johndoe@university.edu"
                   :student/first "Some"}])

; defining 'whole' snapshot
(def db (d/db conn))

(d/basis-t db)
(d/basis-t (d/as-of db t2))
(d/basis-t (d/as-of db 1))

; querying whole db
(db.ent/all-students db)
(db.ent/all-courses db)

; querying as-of
(db.ent/all-students (d/as-of db t1))
(db.ent/all-courses (d/as-of db t1))

; querying since
(db.ent/all-students (d/since db t2))
(db.ent/all-courses (d/since db t2))

; querying a fragment of the db (using as-of and since
(db.ent/all-students (-> db
                         (d/since t1)
                         (d/as-of t2)))
(db.ent/all-courses (-> db
                        (d/since t1)
                        (d/as-of t2)))

; complete verify: counts all entities, attributes and transactions

; as-of verify (from start of db until a certain point):
; - counts only entities created until the time

; since verify (from a certain point until today):
; - counts only entities added after the time

(defn ^:private get-transactions-count [db]
  (reduce + 0 (eduction (map (constantly 1)) (d/datoms db :aevt :db/txInstant))))

(get-transactions-count db-initial-state)                    ; 6
(get-transactions-count db)                                 ; 11 = 6 + 5 transactions
(get-transactions-count (d/as-of db t1))                    ; 6 base + 3 transactions
(get-transactions-count (d/since db t2))                    ; only 1 tx after t2
(get-transactions-count (-> db
                            (d/since t1)
                            (d/as-of t2)))                  ; 1 tx between t1 and t2
(get-transactions-count (-> db
                            (d/since t2)
                            (d/as-of t1)))                  ; impossible db state

; issue: basis-t of a full db = basis-t of an as-of db
(d/basis-t db)
(d/basis-t (d/as-of db t1))

(println t1)
; getting as-of-t seems like a solution!
(d/as-of-t (d/as-of db t1))

; but it gives a tx number that doesn't exist :( (in fact it gives 1 number less than the next tx
(d/t->tx 1010)
(sort (db.ent/all-transactions (d/as-of db t1)))
(sort (db.ent/all-transactions db))


; possible solutions:
; option 1: get a log ranging from 1 to the as-of-t, get tx inst value of last item
(let [as-of-db (d/as-of db t1)
      as-of-t (d/as-of-t as-of-db)
      log (d/log conn)]
  (println (d/t->tx as-of-t))
  (-> log
      (d/tx-range 1 t1)
      last
      :data
      first
      ))

; option 2: get the highest transaction number in the db, then make another query to get its inst
;(d/q '[:find (max ?tx) .
;       :where [?tx :db/txInstant]]
;     (d/as-of db t1))

(-> (d/datoms (d/as-of db t1) :aevt :db/txInstant)
    last
    :v)
