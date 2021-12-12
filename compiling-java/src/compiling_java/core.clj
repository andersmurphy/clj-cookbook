(ns compiling-java.core
    (:gen-class)
  (:import [greatings Greater]))

(defn -main []
  (.great (Greater.)))
