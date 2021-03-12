(ns test-datomic.db.generators
  (:require [clojure.test.check.generators :as gen]
            [schema-generators.generators :as g]
            [schema.core :as s]))

; não existe generator de bigdecimal por padrão, precisamos criar
; estratégia: pegar o generator de double e converter pra bigdec usando fmap
(defn double-para-bigdecimal [valor] (BigDecimal. valor))
(def double-finito (gen/double* {:infinite? false, :NaN? false}))
(def bigdecimal (gen/fmap double-para-bigdecimal double-finito))

;(def char-custom (fmap core/char (choose 0 255)))
;(def str-custom (fmap clojure.string/join (vector char-ascii))
(def leaf-gens {s/Str gen/string-alphanumeric})
