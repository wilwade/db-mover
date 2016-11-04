(ns db-mover.core
  (:gen-class)
  (:require
    [org.httpkit.server :as srv]
    [cheshire.core :as json]
    [db-mover.log :as log]
    [db-mover.move :as move]
    [db-mover.db :as db]
    [db-mover.env :as env]))

(defn- json-200
  [data]
  {:status  200
   :headers {"Content-Type" "text/json"}
   :body    (json/encode data)})

(defn app
  [req]
  (when-not (#{"/favicon.ico"} (:uri req))
    (json-200 {:healthy true
               :status (move/add-current @move/progress)})))

(defn- start-server
  []
  (srv/run-server #'app {:port (:port (env/configuration))}))

(defn- start-movers
  "This should be a list of move/move-at and move/move-multi-at calls"
  []
  (move/move-at 10 "table" "field" :timestamp [:id])
  (move/move-multi-at 10
    [["table" "version" :integer [:id]]
     ["table2" "version" :integer [:table_id :id]]]))

(defn -main
  []
  (start-server)
  (start-movers)
  (log/info "db-mover started"))
