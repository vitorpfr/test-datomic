(ns test-datomic.attribute-migration
  (:require [datomic.api :as d]))

(def db-uri
  ;; (str "datomic:mem://" (d/squuid))
  (str "datomic:dev://localhost:4334/" (d/squuid)))
(d/create-database db-uri)
(def conn (d/connect db-uri))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; schema
(def schema
  [;; dataset
   {:db/ident :dataset/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "The dataset name"}
   {:db/ident :dataset/clearance
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "The dataset clearance"}
   ;; clearances
   {:db/ident :metapod.dataset.clearance/general}
   {:db/ident :metapod.dataset.clearance/pii}
   {:db/ident :metapod.dataset.clearance/raw}])
@(d/transact conn schema)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; data
(def data
  [;; datasets
   {:dataset/name "providers"
    :dataset/clearance :metapod.dataset.clearance/general}
   {:dataset/name "customers"
    :dataset/clearance :metapod.dataset.clearance/pii}
   {:dataset/name "products"
    :dataset/clearance :metapod.dataset.clearance/raw}
   ;; Add duplicates
   {:dataset/name "providers-2"
    :dataset/clearance :metapod.dataset.clearance/general}
   {:dataset/name "customers-2"
    :dataset/clearance :metapod.dataset.clearance/pii}
   {:dataset/name "products-2"
    :dataset/clearance :metapod.dataset.clearance/raw}]
  )
@(d/transact conn data)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; point in time db value
(def db1 (d/db conn))
;; Query for all datasets:
(d/q '[:find ?e ?dataset-name ?dataset-clearance
       :where
       [?e :dataset/name ?dataset-name]
       [?e :dataset/clearance ?dataset-clearance]]
     db1)
(d/entity db1 [:dataset/name "providers"])
(d/touch (d/entity db1 [:dataset/name "providers"]))
;; installs :clearance/name attribute
@(d/transact conn [{:db/ident :clearance/name
                    :db/valueType :db.type/string
                    :db/cardinality :db.cardinality/one
                    :db/unique :db.unique/identity}])
;; Install each type of clearance
@(d/transact conn [{:clearance/name "metapod_dataset_clearance__general"}
                   {:clearance/name "metapod_dataset_clearance__raw"}
                   {:clearance/name "metapod_dataset_clearance__pii"}])
;;Create a mapping between ident keyword and the lookup ref you want it mapped to.
;;This is the most annoying part because it has to be confirmed correct by a human.
(def ident->lookup
  {:metapod.dataset.clearance/general [:clearance/name "metapod_dataset_clearance__general"]
   :metapod.dataset.clearance/raw     [:clearance/name "metapod_dataset_clearance__raw"]
   :metapod.dataset.clearance/pii     [:clearance/name "metapod_dataset_clearance__pii"]})
;; Construct migration tx-data (from the query itself ;)
@(def migration-tx-data
   (into [] (d/q '[:find ?op ?dataset-lookup ?attr ?new-clearance
                   :in $ ?op ?attr ?ident->entity
                   :where
                   [?e :dataset/clearance ?clearance]
                   [?clearance :db/ident ?c-ident]
                   [(get ?ident->entity ?c-ident) ?new-clearance]
                   [?e :dataset/name ?dataset-name]
                   [(vector :dataset/name ?dataset-name) ?dataset-lookup]]
                 db1 :db/add :dataset/clearance ident->lookup)))
;; migrates enum/keyword entity to become a regular entity
@(d/transact conn migration-tx-data)
(def db2 (d/db conn))
(d/q '[:find ?e ?clearance-ref ?clearance-name
       :where
       [?e :dataset/name "providers"]
       [?e :dataset/clearance ?clearance-ref]
       [?clearance-ref :clearance/name ?clearance-name]]
     db2)
;; No keyword ident anymore! Yay!
(d/touch (d/entity db2 [:dataset/name "providers"]))
;Note that the below expression evaluates to true! Yay!
(= (:dataset/clearance (d/touch (d/entity db2 [:dataset/name "providers"])))
   (:dataset/clearance (d/touch (d/entity db2 [:dataset/name "providers-2"]))))
; Who cares about retracting this? Maybe instead you should add a new attribute to this marking it deprecated?
@(d/transact conn [[:db/retract :metapod.dataset.clearance/general :db/ident :metapod.dataset.clearance/general]])
(def db3 (d/db conn))
;Note that the below expression evaluates to true! Yay!
(not= (d/touch (d/entity db3 :metapod.dataset.clearance/general))
      (:dataset/clearance (d/touch (d/entity db3 [:dataset/name "providers"]))))
