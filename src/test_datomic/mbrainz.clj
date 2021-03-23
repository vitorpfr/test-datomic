(ns test-datomic.mbrainz
  (:require [test-datomic.db.config :as db.config]
            [datomic.api :as d]
            [test-datomic.db.data-integrity :as data-integrity]))

; ns dedicated to explore the mbrainz datasets

; to restore them:
; bin/datomic restore-db file:///Users/vitor.freitas/Downloads/mbrainz-1968-1973/ datomic:dev://localhost:4334/mbrainz-1968-1973
; bin/datomic restore-db file:///Users/vitor.freitas/Downloads/datomic-mbrainz-backup-20130611/ datomic:dev://localhost:4334/mbrainz

(def db-uri "datomic:dev://localhost:4334/mbrainz")
(def db-uri-segment "datomic:dev://localhost:4334/mbrainz-1968-1973")

(def uris [db-uri db-uri-segment])

; connect to existing dbs
(def conns (mapv d/connect uris))

(def conn (first conns))
(def conn-segment (second conns))

; check data integrity (are dbs equal?)
(data-integrity/verify db-uri db-uri-segment)
