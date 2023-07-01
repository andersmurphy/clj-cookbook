(ns sqlite.db.HelloWorld
  (:gen-class
   :extends org.sqlite.Function
   :exposes-methods {result superResult}))

(defn -xFunc [this]
  (.superResult this "hello, world!"))
