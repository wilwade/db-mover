(ns db-mover.move
  (:require
    [clojure.string :as str]
    [db-mover.log :as log]
    [db-mover.db :as db]
    [chime :refer [chime-at]]
    [clj-time.core :as t]
    [clj-time.periodic :refer [periodic-seq]]
    [clj-time.coerce :as c]))

(defonce progress (atom {}))

;; Limit of rows per call
(def limit 100)

;; Backup 30 on the interger type
(def integer-less 30)

;; Backup 30 seconds on the timestamp type
(def timestamp-less 30)

(defn- update-progress
  [table field value]
  (when value
    (swap! progress assoc-in [(keyword table) (keyword field)] value)))

(defn- foreach
  "Take a map and pass the key val into function, but return a k-v map"
  [f col]
  (into {} (doall (map (fn [[k v]] {k (f k v)}) col))))

(defn add-current
  [progress]
  (db/with-from-connection from
    (foreach
      (fn [table fields]
        (foreach
          (fn [field to-val]
            (let [from-val (str (db/select-max from (name table) (name field)))
                  to-val (str to-val)]
              {:from from-val
               :to to-val
               :same (= from-val to-val)}))
          fields))
      progress)))

(defn- coerce-value
  [typed val]
  (case typed
    ;; Having a java.sql.Timestamp depends on select val vs. select max(val)
    :timestamp (if (= (type val) java.sql.Timestamp)
                (c/from-sql-time val)
                (c/from-sql-time (java.sql.Timestamp/valueOf ^String val)))
    ;; Having a string depends on select val vs. select max(val)
    :integer (str val)))

(defn- get-progress
  "Gets the current progress from the to database"
  [db table field typed]
  (let [val (or
              (get @progress [(keyword table) (keyword field)])
              (db/select-max db table field))]
    (if (nil? val)
      ;; Defaults
      (case typed
        :timestamp (new java.sql.Timestamp 0)
        :integer "0")
      (let [val (coerce-value typed val)]
        (case typed
          :timestamp (c/to-sql-time
                       (.minusSeconds
                         val
                         timestamp-less))
          :integer (str (- (read-string val) integer-less)))))))

(defn- move-values!
  "Move all the values from one table into another."
  [from to table field unique-fields start offset]
  ;; Note the >= Always we will get something to upsert, but we can never miss any.
  (let [result (db/select*-array from table (str "WHERE \"" field "\" >= ? ORDER BY \"" field "\" ASC LIMIT " limit " OFFSET " offset) start)]
        (when (next result) ;; -array always has the columns returned first
          (db/upsert to table unique-fields result field))))

(defn move-values
  "Move all the values from one table into another."
  [from to table field field-type unique-fields]
  (let [start (get-progress to table field field-type)]
    (loop [pmax nil
           offset 0]
      (let [result (move-values! from to table field unique-fields start offset)
            max (when result ((keyword field) (last result)))]
        (update-progress table field max)
        (if (= limit (count result))
          (recur max (+ limit offset))
          (or max pmax))))))

(defn move
  ([table field field-type unique-fields]
    (db/with-from-connection from
      (db/with-to-connection to
        (move from to table field field-type unique-fields))))
  ([from to table field field-type unique-fields]
    ;; Why wrap in a postgresql exception catcher?
    ;; Foreign Key Race conditions
    (db/wrap-postgres-exception
      (let [current (move-values from to table field field-type unique-fields)]
        (log/debug "Run" {:table table :current-value current})))))

(defn move-multi
  "[[table field field-type unique-fields]]"
  [multi]
  (db/with-from-connection from
    (db/with-to-connection to
      (mapv #(apply move from to %) multi))))

(defn move-at
  "seconds table field field-type unique-fields"
  [every-s table field field-type unique-fields]
  (chime-at (rest (periodic-seq (t/now) (t/seconds every-s)))
    (fn [_]
      (move table field field-type unique-fields))))

(defn move-multi-at
  "seconds [[table field field-type unique-fields], ...]"
  [every-s multi]
  (chime-at (rest (periodic-seq (t/now) (t/seconds every-s)))
    (fn [_]
      (move-multi multi))))
