(ns test-datomic.model
  (:require [schema.core :as s]))

(def Student
  {:student/first s/Str
   :student/last  s/Str
   :student/email s/Str})

(def Course
  {:course/id   s/Str
   :course/name s/Str})

(def Semester
  {:semester/year        java.lang.Long
   :semester/season      s/Keyword})
