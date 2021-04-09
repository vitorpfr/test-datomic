(ns test-datomic.cache.config
  (:require [taoensso.carmine :as car :refer (wcar)]))

;; supported cache: redis
(def cache-uri "redis://localhost:6379/")
(def cache-conn {:pool {} :spec {:uri cache-uri}})

(defmacro wcar* [& body] `(car/wcar cache-conn ~@body))

(defn clear []
  (wcar* (car/flushall)))

(defn set [k v]
  (wcar* (car/set k v)))

(defn get [k]
  (wcar* (car/get k)))
;
;(defn clear-cache []
;  (car/wcar cache-conn (car/flushall)))

;;;; CARMINE LIB DEMO BELOW
; returns ["PONG" "OK" "bar"]
(wcar*
  (car/ping)
  (car/set "foo" "bar")
  (car/get "foo"))

; returns ["PONG"] as pipeline
(wcar* :as-pipeline (car/ping))

; carmine works with complex data types because it does (de)serialization under the hood
; (redis only know byte strings: https://redis.io/topics/data-types
(wcar* (car/set "clj-key" {:bigint (bigint 31415926535897932384626433832795)
                           :vec    (vec (range 5))
                           :set    #{true false :a :b :c :d}
                           :bytes  (byte-array 5)
                           ;; ...
                           })
       (car/get "clj-key"))
