(ns test-datomic.db.entities
  (:require [datomic.api :as d]))

(defn all-students
  [db]
  (d/q '[:find (pull ?e [*])
         :where [?e :student/email]]
       db))

(defn all-semesters
  [db]
  (d/q '[:find (pull ?e [*])
         :where [?e :semester/year+season]]
       db))

(defn all-courses
  [db]
  (d/q '[:find (pull ?e [*])
         :where [?e :course/id]]
       db))

(defn course-count
  [db]
  (d/q '[:find (count ?e) .
         :where [?e :course/id]]
       db))

(defn course-count-optimized
  [db]
  (count (d/qseq {:query '[:find [?e ...]
                           :where [?e :course/id]]
                  :args  [db]})))

(defn course-count-optimized-two
  [db]
  (count (d/qseq {:query '[:find ?e
                           :where [?e :course/id]]
                  :args  [db]})))

(defn course-count-optimized-three
  [db]
  (count (seq (d/datoms db :avet :course/id))))

(defn all-registrations
  [db]
  (d/q '[:find (pull ?e [* {:reg/course [*]} {:reg/student [*]} {:reg/semester [*]}])
         :where [?e :reg/semester+course+student]]
       db))

(defn one
  [db ent]
  (d/pull db '[*] ent))
