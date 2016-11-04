(defproject db-mover "0.1.0"
  :description "Move from one postgresql to another"
  :url "http://github.com/wilwade/db-mover"
  :dependencies
    [[org.clojure/clojure "1.8.0"]
     [org.postgresql/postgresql "9.4.1209"]
     [org.clojure/java.jdbc "0.6.2-alpha3"]
     [honeysql "0.8.0"]
     [http-kit "2.1.19"]
     [cheshire "5.6.1"]
     [swiss-arrows "1.0.0"]
     [clj-time "0.12.0"]
     [jarohen/chime "0.1.9"]
     [environ "1.1.0"]]

  :profiles
  {:uberjar {:aot :all}}
  :uberjar-name "uber-db-mover.jar"

  :main db-mover.core)
