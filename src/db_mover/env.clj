(ns db-mover.env
  (:import
   [java.util TimeZone]
   [java.net InetAddress])
  (:require
    [clojure.java.shell :refer [sh]]
    [environ.core :refer [env]]))

(TimeZone/setDefault (TimeZone/getTimeZone "UTC"))

(defn- hostname [] (-> (sh "hostname") (:out) (.trim)))

(defn local-address [] (.getHostAddress (InetAddress/getLocalHost)))

(declare defaults configuration* configuration)
(when-not *compile-files*
  (def defaults
    {:port 8080
     :host "localhost"
     :env "local"

     :ip       (local-address)
     :hostname (hostname)
     :service-type "db-mover"

     :log-debug false
     :log-info false
     :log-error true

     ;; Read only
     :from-db-host "localhost"
     :from-db-name "from-db"
     :from-db-user "from"
     :from-db-password "from-pw"
     :from-db-port "5432"
     :from-db-sslmode "disable" ;; disable|require

     ;; Read/Write
     :to-db-host "localhost"
     :to-db-name "to-db"
     :to-db-user "to"  ;; RW
     :to-db-password "to-pw"
     :to-db-port "5432"
     :to-db-sslmode "disable" ;; disable|require
     })

(def configuration* (->> defaults
                        (map #(vector (first %) (or (env (first %)) (second %))))
                        (into {})))
(def configuration (constantly configuration*)))
