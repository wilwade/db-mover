(ns db-mover.log
  (:refer-clojure :rename {min core-min})
  (:require
   [db-mover.env :as env]
   [clojure.string :as str]))

(defn- out
  [prefix message data]
  (println (str prefix message "\n" data)))

(defn debug
  ([message] (debug message nil))
  ([message data]
    (when (:log-debug (env/configuration))
      (out "DEBUG: " message data))))

(defn info
  ([message] (info message nil))
  ([message data]
    (when (:log-info (env/configuration))
      (out "INFO: " message data))))

(defn error
  ([message] (error message nil))
  ([message data]
    (when (:log-error (env/configuration))
      (out "ERROR: " message data))))
