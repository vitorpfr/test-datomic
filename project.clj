(defproject test-datomic "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :repositories [["cognitect-dev-tools" {:url      "https://dev-tools.cognitect.com/maven/releases/"
                                         :username :env
                                         :password :env}]]
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [com.datomic/datomic-pro "1.0.6269"]
                 [prismatic/schema "1.1.12"]
                 [org.clojure/test.check "1.1.0"]
                 [prismatic/schema-generators "0.1.3"]
                 [com.clojure-goes-fast/clj-memory-meter "0.1.3"]
                 [com.taoensso/carmine "3.1.0"]
                 ;[com.datomic/dev-local "0.9.232"]
                 ;[com.cognitect/hmac-authn "0.1.195"]
                 ;[com.cognitect/http-client "0.1.105"]
                 ;[com.cognitect/transit-clj "1.0.324"]
                 ;[com.datomic/client-impl-pro "0.8.11"]
                 [com.datomic/client-pro "0.9.66"]]
  ;:repl-options {:init-ns test-datomic.one-db}
  )
