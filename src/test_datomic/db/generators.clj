(ns test-datomic.db.generators
  (:require [clojure.test.check.generators :as gen]
            [schema-generators.generators :as g]
            [schema.core :as s]))

(def anything-but-empty-string (gen/such-that #(not= % "") gen/string-alphanumeric))
(def leaf-gens {s/Str anything-but-empty-string})
