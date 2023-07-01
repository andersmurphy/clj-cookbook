(ns sqlite.db.HelloWorld
  (:gen-class
   :extends org.sqlite.Function
   :exposes-methods {result superResult})
  (:import org.sqlite.Function))

(defn -xFunc [this]
  (println "hello world"))
