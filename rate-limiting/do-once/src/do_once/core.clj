(ns do-once.core
  (:require [next.jdbc :as jdbc]))

(def db {:dbtype "postgresql" :dbname "databasename"})
(def ds (jdbc/get-datasource db))

(defn done? [uuid name]
  (jdbc/execute-one! ds ["
select * from do_once where uuid = ? and name = ?"
                         uuid name]))

(defn do! [uuid name]
  (jdbc/execute! ds ["
insert into do_once (uuid, name) values (? , ?) on conflict (uuid, name) do nothing"
                     uuid name]))

(defmacro do-once! [uuid name & body]
  `(when-not (done? ~uuid ~name)
     (do! ~uuid ~name)
     ~@body))

(defmacro do-once-2! [& {:keys [uuid name action]}]
  `(when-not (done? ~uuid ~name)
     (do! ~uuid ~name)
     ~action))

(defn do-once-3! [{:keys [uuid name action]}]
  (when-not (done? uuid name)
    (do! uuid name)
    (action)))

(comment
  (jdbc/execute! ds ["
create table do_once (
  pid serial primary key,
  uuid text not null,
  name text not null)"])

  (jdbc/execute! ds ["
create unique index do_once_unique ON do_once(uuid, name)"])

  (do-once! "Nora" "email-2020-02-08"
            (println "email sent")
            (prn (+ 1 2 3 4)))

  (do-once-2! :uuid "Nora"
              :name "email-2020-02-09"
              :action (do (println "email sent")
                          (prn (+ 1 2 3 4))))

  (do-once-3! {:uuid   "Nora"
               :name   "email-2020-02-10"
               :action (fn []
                         (println "email sent")
                         (prn (+ 1 2 3 4)))}))
