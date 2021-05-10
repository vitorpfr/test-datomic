(ns test-datomic.t-evolution
  (:require [datomic.api :as d]
            [schema-generators.generators :as g]
            [test-datomic.db.config :as db.config]
            [test-datomic.db.entities :as db.ent]
            [test-datomic.db.util :as db.util]
            [clojure.pprint :as pp]
            [schema.core :as s]))

(def uri "datomic:dev://localhost:4334/school-original")

(defn transact-and-print [conn content]
  (let [data @(d/transact conn content)]
    (println "tx eid" (-> data :tx-data first :e))
    (println "tempids" (-> data :tempids))
    (println "basis-t" (-> conn d/db d/basis-t) "next-t" (-> conn d/db d/next-t))
    (println "--------------------------------------------------")))

(def schema [{:db/ident       :course/id
              :db/valueType   :db.type/uuid
              :db/cardinality :db.cardinality/one
              :db/unique      :db.unique/identity}
             {:db/ident       :course/name
              :db/valueType   :db.type/string
              :db/cardinality :db.cardinality/one}
             {:db/ident       :course/sth
              :db/valueType   :db.type/ref
              :db/cardinality :db.cardinality/one}
             {:db/ident       :course/sth-two
              :db/valueType   :db.type/ref
              :db/cardinality :db.cardinality/one}])


(def conn (db.config/restart-db! uri))
(println "initial basis-t" (-> conn d/db d/basis-t) "next-t" (-> conn d/db d/next-t))
(transact-and-print conn schema)

(transact-and-print conn [{:db/ident       :test/id
                           :db/valueType   :db.type/uuid
                           :db/cardinality :db.cardinality/one
                           :db/unique      :db.unique/identity}
                          {:db/ident       :test/name
                           :db/valueType   :db.type/string
                           :db/cardinality :db.cardinality/one}
                          {:db/ident       :audit/name
                           :db/valueType   :db.type/string
                           :db/cardinality :db.cardinality/one}])
(println "disclaimer: no matter how many attributes we add, it doesn't increase the t jump as entities do (just + 1 for the transaction itself")

(defn generate-courses [conn how-many-times sample-size]
  (dotimes [n how-many-times]
    (let [courses (g/sample sample-size {:course/id   s/Uuid
                                         :course/name s/Str})]
      (transact-and-print conn (conj courses [:db/add "datomic.tx" :audit/name "eu"])))))

(defn course [] [{:course/id   (java.util.UUID/randomUUID)
                  :course/name "aa"}])

(defn generate-courses-parallel [conn how-many-times]
  (let [courses-part (repeatedly how-many-times course)
        courses      (shuffle (into courses-part [[{:db/id      "datomic.tx"
                                                    :audit/name "oi"}
                                                   {:db/id     "123"
                                                    :test/id   (java.util.UUID/randomUUID)
                                                    :test/name "aaaaa"}
                                                   {:db/id      17592186045427
                                                    :course/sth "123"}
                                                   {:db/id          17592186045427
                                                    :course/sth-two "123"}]]))]
    (doall (pmap #(d/transact conn %) courses))))

; regras extraídas:
; o próximo next-t é igual ao next-t atual + o número de novas entidades + 1 (a transação)
; logo, se uma transação só modifica entidades existentes, necessariamente o next-t vai aumentar em 1

; preciso criar uma entidade e referenciar ela numa entidade que já existe

;(d/transact conn [[:db/add 17592186045423 :course/name "oi"]
;                  [:db/retract 17592186045423 :course/name ""]])

(generate-courses conn 5 1)


(let [tempid (d/tempid :db.part/user)]
  (transact-and-print conn [{:db/id      "datomic.tx"
                             :audit/name "oi"}
                            {:db/id     tempid
                             :test/id   (java.util.UUID/randomUUID)
                             :test/name "aaaaa"}
                            {:db/id      17592186045427
                             :course/sth tempid}
                            {:db/id          17592186045427
                             :course/sth-two tempid}]))

(let [tempid (d/tempid :db.part/user)]
  (transact-and-print conn [{:db/id      "datomic.tx"
                             :audit/name "eu"}
                            {:db/id     tempid
                             :test/id   (java.util.UUID/randomUUID)
                             :test/name "bbbbb"}
                            {:db/id      17592186045427
                             :course/sth tempid}
                            {:db/id       17592186045427
                             :course/name "outro nome"}]))
;;
(generate-courses conn 1 1)

(generate-courses-parallel conn 10000)

(db.util/all-txs (d/log conn) (d/db conn) 1000 50)

; CONCLUSIONS:
; t seems to have a pattern to grow (next next-t is number of new entities added in a tx + 1)
; however, datomic itself cannot guarantee that this behavior will always happen in production dbs
; so we can't rely on predicting the next t




