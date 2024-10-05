(ns app.core
  (:gen-class)
  (:require [ring.adapter.jetty :refer [run-jetty]])
  (:import (java.util.concurrent Executors)
           (org.eclipse.jetty.util.thread QueuedThreadPool)))

(defn handler [req]
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body    "<p>hello</p>"})

(defn start-server []
  (let [thread-pool (new QueuedThreadPool)
          _           (.setVirtualThreadsExecutor thread-pool
                        (Executors/newVirtualThreadPerTaskExecutor))]
    (run-jetty handler
      {:port        8080
       :join?       false
       :thread-pool thread-pool})))

(comment
  (def server (start-server))

  (.stop server))

;; oha -c 100 -z 10s -m GET http://localhost:8080
