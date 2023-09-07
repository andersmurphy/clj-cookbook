(ns server.core
  (:gen-class)
  (:require [muuntaja.core :as m]
            [reitit.ring :as ring]
            [reitit.coercion.spec]
            [reitit.ring.coercion :as rrc]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as parameters]
            [ring.adapter.jetty :refer [run-jetty]]
            [reitit.coercion.malli :as malli]
            [lambdaisland.hiccup :as h])
  (:import (java.util.concurrent Executors)
           (org.eclipse.jetty.util.thread QueuedThreadPool)))

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
  (def jetty-server (run-jetty app {:port 8080 :join? false}))

  (def jetty-server
    (let [thread-pool (new QueuedThreadPool)
          _           (.setVirtualThreadsExecutor thread-pool
                        (Executors/newVirtualThreadPerTaskExecutor))]
      (run-jetty app {:port        8080
                      :join?       false
                      :thread-pool thread-pool})))

  (.stop jetty-server))

;; Jetty

;; No virtual threads
;; Running 10s test @ http://127.0.0.1:8080/hello
;;   12 threads and 120 connections
;; Requests/sec:  79032.31
;; Transfer/sec:      8.74MB

;; Virtual threads
;; Running 10s test @ http://127.0.0.1:8080/hello
;;   12 threads and 120 connections
;; Requests/sec:  73505.95
;; Transfer/sec:      8.13MB

;; No virtual threads each handler has a 50ms thread sleep
;; Running 10s test @ http://127.0.0.1:8080/hello
;;   12 threads and 120 connections
;; Requests/sec:    842.92
;; Transfer/sec:     95.49KB

;; Virtual threads each handler has a 50ms thread sleep
;; Running 10s test @ http://127.0.0.1:8080/hello
;;   12 threads and 120 connections
;; Requests/sec:   2218.19
;; Transfer/sec:    251.28KB

;; 2.6x
