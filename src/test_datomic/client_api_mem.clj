(ns test-datomic.client-api-mem
  (:require [datomic.client.api :as d]
            [clojure.pprint :as pp]))

; REQUIREMENTS:
; - start peer server: bin/run -m datomic.peer-server -h localhost -p 8998 -a myaccesskey,mysecret -d hello,datomic:mem://hello

(def client (d/client {:server-type        :peer-server
                       :access-key         "myaccesskey"
                       :secret             "mysecret"
                       :endpoint           "localhost:8998"
                       :validate-hostnames false}))

(def conn (d/connect client {:db-name "hello"}))

(pp/pprint conn)

; conn is working, here's a test to confirm
(d/transact conn {:tx-data [{:db/ident       :course/id
                             :db/valueType   :db.type/uuid
                             :db/unique      :db.unique/identity
                             :db/cardinality :db.cardinality/one}
                            {:db/ident       :student/id
                             :db/valueType   :db.type/uuid
                             :db/unique      :db.unique/identity
                             :db/cardinality :db.cardinality/one}]})

(d/transact conn {:tx-data [{:course/id (java.util.UUID/randomUUID)}]})

; stats are working correctly
(pp/pprint (d/db-stats (d/db conn)))
