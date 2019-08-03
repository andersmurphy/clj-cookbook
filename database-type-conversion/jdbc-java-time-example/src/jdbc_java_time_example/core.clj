(ns jdbc-java-time-example.core
  (:require [clojure.java.jdbc :as jdbc])
  (:import [java.sql Timestamp]
           [java.sql Date]
           [java.time.format DateTimeFormatter]
           [java.time LocalDate]
           [java.time Instant]
           [java.io FileWriter]))

(extend-protocol jdbc/IResultSetReadColumn
  java.sql.Timestamp
  (result-set-read-column [v _ _]
    (.toInstant v))
  java.sql.Date
  (result-set-read-column [v _ _]
    (.toLocalDate v)))

(extend-protocol jdbc/ISQLValue
  java.time.Instant
  (sql-value [v]
    (Timestamp/from v))
  java.time.LocalDate
  (sql-value [v]
    (Date/valueOf v)))

(defn parse-date [string]
  (LocalDate/parse string))

(defn parse-time [string]
  (and string (-> (.parse (DateTimeFormatter/ISO_INSTANT) string)
                  Instant/from)))

(defmethod print-method java.time.Instant
  [inst out]
  (.write out (str "#time/inst \"" (.toString inst) "\"") ))

(defmethod print-dup java.time.Instant
  [inst out]
  (.write out (str "#time/inst \"" (.toString inst) "\"") ))

(defmethod print-method LocalDate
  [^LocalDate date ^FileWriter out]
  (.write out (str "#time/ld \"" (.toString date) "\"")))

(defmethod print-dup LocalDate
  [^LocalDate date ^FileWriter out]
  (.write out (str "#time/ld \"" (.toString date) "\"")))

(def database-connection "postgresql://localhost:5432/databasename")

(comment
  (jdbc/execute!
   database-connection
   "CREATE TABLE event (pid SERIAL PRIMARY KEY, name text,
   created timestamp with time zone,
   log_date date )")

  (jdbc/insert!
   database-connection
   :event
   {:name     "page_viewed"
    :created  (Instant/now)
    :log_date (LocalDate/now)})

  (jdbc/insert!
   database-connection
   :event
   {:name     "page_viewed"
    :created  #time/inst "2019-08-03T16:28:25.935Z"
    :log_date #time/ld "2019-08-03"})

  (jdbc/query
   database-connection
   ["select * from event"]))
