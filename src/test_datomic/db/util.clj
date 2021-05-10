(ns test-datomic.db.util
  (:require [datomic.api :as d])
  (:import [java.util.concurrent ExecutionException]))

(defn- retry?
  [ex-data]
  (boolean (or (-> ex-data :db/error #{:db.error/transactor-unavailable :db.error/transaction-timeout})
               (-> ex-data :cognitect.anomalies/category #{:cognitect.anomalies/unavailable
                                                           :cognitect.anomalies/interrupted
                                                           :cognitect.anomalies/busy}))))

(defn- maybe-rethrow
  [ex debug-fn]
  (let [data (ex-data ex)]
    (cond
      (retry? data) nil
      :else         (do
                      (debug-fn)
                      (throw ex)))))

(defn retry
  ([msg op]
   (retry msg op (constantly nil)))
  ([msg op debug-fn]
   (let [f #(try
              (op)
              (catch clojure.lang.ExceptionInfo ex
                (maybe-rethrow ex debug-fn))
              (catch ExecutionException ex
                (maybe-rethrow (or (.getCause ex) ex) debug-fn)))]
     (loop []
       (if-some [res (f)]
         res
         (do
           (println "retry" msg)
           (Thread/sleep 3000)
           (recur)))))))

(defn series
  "Produces a sequence of values.
   `f` is a function that given a value, returns the next value.
   `f` called with no arguments produces the initial value.
   `continue?` is a predicate that given a value, determines whether
    to produce the next value. Always produces at least one value."
  [f continue?]
  (reify
    clojure.lang.Seqable
    (seq [_]
      ((fn step [seed]
         (cons seed
               (when (continue? seed)
                 (lazy-seq (step (f seed))))))
       (f)))
    clojure.lang.IReduceInit
    (reduce [_ rf init]
      (loop [seed (f)
             ret (rf init seed)]
        (if (reduced? ret)
          @ret
          (if (continue? seed)
            (let [next (f seed)]
              (recur next (rf ret next)))
            ret))))
    clojure.lang.Sequential))

(defn all-txs
  "Returns all transactions in db from `start`, grabbing transactions `limit` at a time"
  [log db start limit]
  (let [max-t (d/next-t db)]
    (->> (series (fn
                        ([]
                         ;; initial chunk
                         (retry "initial chunk"
                                     #(vec (take limit (d/tx-range log start max-t)))))
                        ([chunk]
                         ;; next chunk begins at t = previous + 1
                         (let [last-t (:t (peek chunk))]
                           (retry (str "grabbing next chunk after " last-t)
                                       #(vec (take limit (d/tx-range log (inc last-t) max-t)))))))
                      (fn [chunk]
                        (when-let [peek-t (:t (peek chunk))]
                          (< (inc peek-t) max-t))))
         ;; flatten the chunks
         (eduction cat))))
