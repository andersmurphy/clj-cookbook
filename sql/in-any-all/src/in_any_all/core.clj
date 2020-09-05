(ns in-any-all.core
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [next.jdbc.prepare :as p])
  (:import [java.sql PreparedStatement]))

(def db {:dbtype "postgresql" :dbname "databasename"})
(def ds (jdbc/get-datasource db))

(extend-protocol p/SettableParameter
 clojure.lang.IPersistentVector
   (set-parameter [v ^PreparedStatement s i]
     (let [conn      (.getConnection s)
           meta      (.getParameterMetaData s)
           type-name (.getParameterTypeName meta i)]
       (if-let [elem-type (when (= (first type-name) \_)
                            (apply str (rest type-name)))]
         (.setObject s i (.createArrayOf conn elem-type (to-array v)))
         (.setObject i s v)))))

(comment
 (jdbc/execute!
  ds
  ["create table user_info (pid serial primary key, name text not null)"])
 (jdbc/execute! ds ["create unique index user_info_unique ON user_info(name)"])
 (sql/insert! ds :user_info {:name "Bob"})
 (sql/insert! ds :user_info {:name "Jane"})
 (sql/insert! ds :user_info {:name "Megan"})
 (sql/insert! ds :user_info {:name "Alice"})
 (sql/query ds ["select * from user_info where name in(?, ?)" "Bob" "Jane"])
 (sql/query ds
            ["select * from user_info where name = any(?)"
             (into-array String ["Bob" "Jane"])])
 (sql/query ds
            ["select * from user_info where name != all(?)"
             (into-array String ["Bob" "Jane"])])
 (sql/query ds ["select * from user_info where name = any(?)" ["Bob" "Jane"]])
 (sql/query ds ["select * from user_info where name != all(?)" ["Bob" "Jane"]])
 (sql/query ds ["select * from user_info where pid != all(?)" [1 2]])
 (sql/query ds ["select * from user_info where pid = any(?)" [1 2]]))
