(ns app.core
  (:gen-class)
  (:require [org.httpkit.server :as hk-server]))

(defn handler [req]
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body    "<p>hello</p>"})

(defn start-server []
  (hk-server/run-server handler
    {:port        8080}))

(comment
  (def server (start-server))

  (server))

;; oha -c 100 -z 10s -m GET http://localhost:8080

