(ns test-datomic.db.generators
  (:require [clojure.test.check.generators :as gen]
            [schema-generators.generators :as g]
            [schema.core :as s]))

(def anything-but-empty-string (gen/such-that #(not= % "") gen/string-alphanumeric))
(def any-int-but-zero (gen/fmap inc gen/nat))
(def leaf-gens {s/Str anything-but-empty-string
                s/Int any-int-but-zero})
