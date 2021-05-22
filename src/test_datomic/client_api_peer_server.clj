(ns test-datomic.client-api-peer-server
  (:require [datomic.client.api :as d]
            [schema.core :as s]
            [schema-generators.generators :as g]))

;;;;;;;;;;;;;;;;;
;; SINGLE TEST ;;
;;;;;;;;;;;;;;;;;

; REQUIREMENTS:
; - have a transactor running in localhost:4334
; - have a table created by this transactor (e.g. school, or huge-school)
; - start peer server: bin/run -m datomic.peer-server -h localhost -p 8998 -a myaccesskey,mysecret -d huge-school,datomic:dev://localhost:4334/huge-school

(defn my-single-verify []
  (let [client (d/client {:server-type        :peer-server
                          :access-key         "myaccesskey"
                          :secret             "mysecret"
                          :endpoint           "localhost:8998"
                          :validate-hostnames false})
        conn   (d/connect client {:db-name "school"})
        db     (d/db conn)]
    [(d/db-stats db) (d/db-stats db)]))

(time (my-single-verify))
;
(def client (d/client {:server-type        :peer-server
                       :access-key         "myaccesskey"
                       :secret             "mysecret"
                       :endpoint           "localhost:8998"
                       :validate-hostnames false}))
;;
(def conn (d/connect client {:db-name "school"}))

;;;;;;;;;;;;;;;;;;
;; REPLICA TEST ;;
;;;;;;;;;;;;;;;;;;

; REQUIREMENTS:
; - have a transactor running in localhost:4334
; - have a table created by this transactor (e.g. school, or huge-school)
; - have this table replicated in the target in localhost:5334 with a transactor
; - start source peer server: bin/run -m datomic.peer-server -h localhost -p 8998 -a myaccesskey,mysecret -d huge-school,datomic:dev://localhost:4334/huge-school
; - start target peer server: bin/run -m datomic.peer-server -h localhost -p 8999 -a myaccesskey,mysecret -d huge-school-replica,datomic:dev://localhost:5334/huge-school-replica


(defn my-pair-verify []
  (let [source-client (d/client {:server-type        :peer-server
                                 :access-key         "myaccesskey"
                                 :secret             "mysecret"
                                 :endpoint           "localhost:8998"
                                 :validate-hostnames false})
        target-client (d/client {:server-type        :peer-server
                                 :access-key         "myaccesskey"
                                 :secret             "mysecret"
                                 :endpoint           "localhost:8999"
                                 :validate-hostnames false})
        [source-conn target-conn] [(d/connect source-client {:db-name "huge-school"})
                                   (d/connect target-client {:db-name "huge-school-replica"})]
        [source-db target-db] [(d/db source-conn) (d/db target-conn)]
        [source-stats target-stats] [(d/db-stats source-db) (d/db-stats target-db)]]
    (println source-stats)
    (println target-stats)
    (println (= source-stats target-stats))))

;(time (my-pair-verify))

; next steps:
; - measure time diff between verify and db-stats (5000 ms vs 32 ms)
; - replicate huge school
; - connect to both original and replica and compare their db-stats to see if they match




