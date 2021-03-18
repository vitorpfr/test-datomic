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

(defn all-registrations
  [db]
  (d/q '[:find (pull ?e [* {:reg/course [*]} {:reg/student [*]} {:reg/semester [*]}])
         :where [?e :reg/semester+course+student]]
       db))

(defn one
  [db ent]
  (d/pull db '[*] ent))
