(ns test-datomic.component-entities
  (:use clojure.pprint)
  (:require [datomic.api :as d]
            [schema.core :as s]
            [test-datomic.db.entities :as db.ent]
            [test-datomic.db.config :as db.config]
            [schema-generators.generators :as g])
  (:import (java.util UUID)))

;https://blog.datomic.com/2013/06/component-entities.html

(s/set-fn-validation! true)

(def db-uri "datomic:dev://localhost:4334/ecommerce")
(def conn (db.config/restart-db! db-uri))

; transact schema
(def schema [{:db/ident       :order/id
              :db/valueType   :db.type/uuid
              :db/cardinality :db.cardinality/one
              :db/unique      :db.unique/identity}

             {:db/ident       :order/line-items
              :db/valueType   :db.type/ref
              :db/isComponent true
              :db/cardinality :db.cardinality/many}

             {:db/ident       :line-item/product
              :db/valueType   :db.type/string
              :db/cardinality :db.cardinality/one}
             {:db/ident       :line-item/quantity
              :db/valueType   :db.type/long
              :db/cardinality :db.cardinality/one}
             {:db/ident       :line-item/price
              :db/valueType   :db.type/double
              :db/cardinality :db.cardinality/one}
             ])
(d/transact conn schema)

; transact a order with the line items
(def order-uuid (UUID/randomUUID))
(def order-one {:order/id         order-uuid
                :order/line-items [{:line-item/product  "Chocolate"
                                    :line-item/quantity 1
                                    :line-item/price    48.00}

                                   {:line-item/product  "Whisky"
                                    :line-item/quantity 2
                                    :line-item/price    38.00}]})
(d/transact conn [order-one])

; check that the order entity is there with the sub components (line items)
(d/q '[:find (pull ?e [*])
       :where
       [?e :order/id]]
     (d/db conn))

(d/q '[:find (pull ?e [*])
       :where
       [?e :line-item/product]]
     (d/db conn))

; if I retract the order entity, the line items are also retracted
(clojure.pprint/pprint (d/transact conn [[:db/retractEntity [:order/id order-uuid]]]))

; check that all entities were retracted (order and line items)
(d/q '[:find (pull ?e [*])
       :where
       [?e :order/id]]
     (d/db conn))

(d/q '[:find (pull ?e [*])
       :where
       [?e :line-item/product]]
     (d/db conn))
