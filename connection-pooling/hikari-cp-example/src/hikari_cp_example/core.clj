(ns hikari-cp-example.core
  (:require [clojure.java.jdbc :as jdbc]
            [hikari-cp.core :as cp]
            [clojure.string :as str]))

(defn db-info-from-url [db-url]
  (let [db-uri              (java.net.URI. db-url)
        [username password] (str/split (or (.getUserInfo db-uri) ":") #":")]
    {:username      (or username (System/getProperty "user.name"))
     :password      (or password "")
     :port-number   (.getPort db-uri)
     :database-name (str/replace-first (.getPath db-uri) "/" "")
     :server-name   (.getHost db-uri)}))

(def datasource-options
  (merge (db-info-from-url "postgresql://localhost:5432/databasename")
         {:auto-commit        true
          :read-only          false
          :adapter            "postgresql"
          :connection-timeout 30000
          :validation-timeout 5000
          :idle-timeout       600000
          :max-lifetime       1800000
          :minimum-idle       10
          :maximum-pool-size  20
          :pool-name          "db-pool"
          :register-mbeans    false}))

(defonce datasource
  (delay (cp/make-datasource datasource-options)))

(def database-connection {:datasource @datasource})

(comment
  (clojure.java.jdbc/execute!
   database-connection
   "CREATE TABLE user_info (pid SERIAL PRIMARY KEY, name text)")

  (clojure.java.jdbc/insert!
   database-connection
   :user_info
   {:name "anders"}))
