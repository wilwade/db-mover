(ns db-mover.db
  (:require
    [db-mover.env :as env]
    [db-mover.log :as log]
    [clojure.string :as str]
    [clojure.set :as set]
    [clojure.java.jdbc :as j]))

(defmacro wrap-postgres-exception
  [& body]
  `(try
    ~@body
    (catch org.postgresql.util.PSQLException e#
      (log/info "postgresql exception" {:e (.getMessage e#)}))))

(defn- ks
  [& more]
  (keyword (apply str more)))

(defn- db*
  [prefix config]
  {:classname "org.postgresql.Driver"
   :subprotocol "postgresql"
   :subname (str "//" ((ks prefix "-db-host") config)
                 ":" ((ks prefix "-db-port") config)
                 "/" ((ks prefix "-db-name") config))
   :user ((ks prefix "-db-user") config)
   :password ((ks prefix "-db-password") config)
   :sslmode ((ks prefix "-db-sslmode") config)})

(declare from to)
(when-not *compile-files*
  (defonce from (db* "from" (env/configuration)))

  (defonce to (db* "to" (env/configuration))))

(defmacro with-to-connection
  [bound & body]
  `(j/with-db-connection [~bound to]
     ~@body))

(defmacro with-from-connection
  [bound & body]
  `(j/with-db-connection [~bound from]
     ~@body))

(defn- str-if
  [v]
  (when v
    (str v)))

(defn select-max
  "return the max value from a table"
  [db table field]
  (->>
    [(str "SELECT MAX(\"" field "\") AS \"max\" FROM \"" table "\"")]
    (j/query db)
    first
    :max
    str-if))

(defn select*
  "Simple select statement creator returns a map"
  [db table & [where & params]]
  (j/query db (cons (str "SELECT * FROM \"" table "\" " where) params)))

(defn select*-array
  "Simple select statement creator returns a map"
  [db table & [where & params]]
  (j/query db (cons (str "SELECT * FROM \"" table "\" " where) params) {:as-arrays? true}))

(defn upsert
  "Simple upsert statement creator, always has all the fields
  returning can be the field from the inserted rows needed to return"
  ([db table unique-fields result returning] (upsert db table unique-fields (first result) (rest result) returning))
  ([db table unique-fields cols rows returning]
  (let [set-cols (map name (set/difference (into #{} cols) (into #{} unique-fields)))
        col-count (count cols)
        row-count (count rows)
        q (into [(str "INSERT INTO \"" table "\" ("
                      (str/join ", " (map #(str "\"" (name %) "\"") cols))
                      ") VALUES "
                      (str/join ", "
                        (repeat row-count
                          (str "(" (str/join ", " (repeat col-count "?")) ")")))
                      " ON CONFLICT ("
                      (str/join ", " (map #(str "\"" (name %) "\"") unique-fields))
                      ") DO UPDATE SET "
                      (str/join ", "
                        (map #(str "\""%"\" = EXCLUDED.\"" % "\"") set-cols))
                  (when returning
                    (str " RETURNING \"" returning "\"")))]
            (flatten rows))]
    (j/query db q))))
