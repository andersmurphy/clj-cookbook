(ns server.core
  (:gen-class)
  (:require [muuntaja.core :as m]
            [org.httpkit.server :as hk-server]
            [reitit.ring :as ring]
            [reitit.coercion.spec]
            [reitit.ring.coercion :as rrc]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as parameters]
            [reitit.coercion.malli :as malli]
            [lambdaisland.hiccup :as h])
  (:import (java.util.concurrent Executors)))

(def app
  (ring/ring-handler
    (ring/router
      ["/hello"
       {:get {:handler (fn [_]
                         {:headers {"Content-Type" "text/html"}
                          :status  200
                          :body    (do
                                    ;; simulate work by sleeping
                                    ;; for 50 milliseconds
                                    (Thread/sleep 50)
                                    (h/render [:b "hello!"]))})}}]
      {:data {:coercion   malli/coercion
              :muuntaja   m/instance
              :middleware [parameters/parameters-middleware
                           rrc/coerce-request-middleware
                           muuntaja/format-response-middleware
                           rrc/coerce-response-middleware]}})))

(comment
  
  (def server (hk-server/run-server #'app
                {:port        8080
                 ;; Use virtual threads
                 :worker-pool (Executors/newVirtualThreadPerTaskExecutor)}))

  (def server (hk-server/run-server #'app
                {:port 8080}))
  ;; stop server
  (server))

;; HTTP kit

;; No virtual threads
;; Running 10s test @ http://127.0.0.1:8080/hello
;;   12 threads and 120 connections
;; Requests/sec:  83074.61
;; Transfer/sec:     11.73MB

;; Virtual threads
;; Running 10s test @ http://127.0.0.1:8080/hello
;;   12 threads and 120 connections
;; Requests/sec:  85202.97
;; Transfer/sec:     12.03MB

;; No virtual threads each handler has a 50ms thread sleep
;; Running 10s test @ http://127.0.0.1:8080/hello
;;   12 threads and 120 connections
;; Requests/sec:     76.00
;; Transfer/sec:     10.98KB

;; Virtual threads each handler has a 50ms thread sleep
;; Running 10s test @ http://127.0.0.1:8080/hello
;;   12 threads and 120 connections
;; Requests/sec:   2208.67
;; Transfer/sec:    319.22KB

;; 29x
