(ns app.core
  (:gen-class)
  (:require [ring.adapter.jetty9 :refer [run-jetty]]
            [clojure.java.io :as io]
            [clojure.core.async :as a]
            [ring.core.protocols :refer [StreamableResponseBody]]
            [clojure.core.async.impl.channels])
  (:import (java.util.concurrent Executors)
           (org.eclipse.jetty.util.thread QueuedThreadPool)
           (org.eclipse.jetty.server Server)
           (java.io OutputStream)
           (java.util.concurrent Executors)))

;; Extend core.async channel with StreamableResponseBody
(extend-type clojure.core.async.impl.channels.ManyToManyChannel
  StreamableResponseBody
  (write-body-to-stream [ch _response ^OutputStream output-stream]
    (with-open [out    output-stream
                writer (io/writer out)]
      (try
        (loop []
          (when-let [^String msg (a/<!! ch)]
            (doto writer (.write msg) (.flush))
            (recur)))
        ;; If the client disconnects writing to the output stream
        ;; throws an IOException.
        (catch java.io.IOException _)
        ;; Close channel after client disconnect.
        (finally (a/close! ch))))))

(defonce clients (atom #{}))

(defn format-event [body]
  (str "data: " body "\n\n"))

(defn send>!! [ch message]
  (let [v (a/>!! ch message)]
    (when-not v (swap! clients disj ch))
    ;; Keeps the return semantics of >!!
    v))

(defn heartbeat>!! [ch msec]
  (Thread/startVirtualThread
    #(loop []
       (Thread/sleep ^long msec)
       (when (send>!! ch "\n\n")
         (recur)))))

(defn handler-sse [_]
  (let [ch (a/chan 10)]
    (swap! clients conj ch)
    (send>!! ch (format-event "Successfully connected"))
    ;; Every 10 seconds we send a heartbeat to check if output stream
    ;; is still open.
    (heartbeat>!! ch 10000)
    {:status  200
     :headers {"Content-Type"  "text/event-stream;charset=UTF-8"
               "Cache-Control" "no-cache, no-store"}
     :body    ch}))

(defn broadcast-message-to-connected-clients! [message]
  (run! (fn [ch] (send>!! ch (format-event message))) @clients))

(def app
  (fn handler [{:keys [request-method uri] :as req}]
    (if (= [:get  "/"] [request-method uri])
      (handler-sse req)
      {:status 404})))

(defn start-server [& {:as opts}]
  (let [thread-pool (new QueuedThreadPool)
        _           (.setVirtualThreadsExecutor thread-pool
                      (Executors/newVirtualThreadPerTaskExecutor))]
    (run-jetty  #'app
      (merge
        {:port        8080
         :thread-pool thread-pool}
        opts))))

(defn -main [& _]
  (start-server))

(comment ;; local development only
  (def server
    (start-server :join? false))
  ;; http://localhost:8080/

  ;; stop server
  (Server/.stop server))

(comment
  ;; Open a terminal and connect
  ;; curl localhost:8080 -vv
  
  (broadcast-message-to-connected-clients! "Hello")
  
  @clients

  )
