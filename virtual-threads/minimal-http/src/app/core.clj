(ns app.core
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import (java.net ServerSocket SocketException Socket)
           (java.io InputStream OutputStream BufferedReader)))

(def responses
  {200 "HTTP/1.1 200 OK\r\n"
   301 "HTTP/1.1 301 Moved Permanently\n"
   404 "HTTP/1.1 404 Not Found\r\n"})
  
(defprotocol StreamableResponseBody
  (write-body-to-stream [body response output-stream]))

(extend-protocol StreamableResponseBody
  byte/1
  (write-body-to-stream [body _ ^OutputStream output-stream]
    (.write output-stream ^bytes body)
    (.close output-stream))
  String
  (write-body-to-stream [body _ ^OutputStream output-stream]
    (doto (io/writer output-stream)
      (.write body)
      (.close)))
  InputStream
  (write-body-to-stream [body _ ^OutputStream output-stream]
    (with-open [body body]
      (io/copy body output-stream))
    (.close output-stream)))

(defn parse-request [^BufferedReader r]
  (loop [line    (.readLine r)
         request {}]
    (if (seq (str/trim line))
      (if (str/starts-with? line "GET")
        (let [[request-method uri protocol] (str/split line #" ")]
          (recur (.readLine r)
            (assoc request
              :request-method request-method
              :uri uri
              :protocol protocol)))
        (let [[k v] (str/split line #":")]
          (recur (.readLine r) (assoc request :headers {k v}))))
      request)))

(defn send-response [^OutputStream out response]
  (write-body-to-stream
    (str
      (get responses (:status response))
      (apply str (for [[k v] (:headers response)]
                   (str k " " v "\r\n")))
      "\r\n"
      (when (:body response)
        (:body response)))
    response
    out))

(defn thread [f]
  (.start (Thread. ^Runnable f)))

(defn run-adapter [handler options]
  (thread
    (fn []
      (let [^ServerSocket server (ServerSocket. (:port options))]
        (try
          (while (not (.isClosed server))
            (let [^Socket conn (.accept server)]
              (Thread/startVirtualThread
                #(try
                   (let [^InputStream in   (io/reader
                                             (.getInputStream conn))
                         ^OutputStream out (.getOutputStream conn)]
                     (send-response out
                       (handler (parse-request in))))
                   (catch SocketException _disconnect)))))
          (catch SocketException _disconnect)
          (finally (.close server)))))))

(defn handler [req]
  {:status  200
   :headers {"Content-Type:" "text/html"}
   :body    "<p>hello</p>"})

(defn start-server []
  (run-adapter
    handler
    {:port 8080}))

(comment
  (def server (start-server))
  
  (server))

;; minimal-jetty
;; Requests/sec:	59548.7883
;; minimal-http-kit
;; Requests/sec:	83047.3862
;; minimal-http
;; Requests/sec:	513.7097
;; Interestingly virtual threads are no more faster than real threads
;; wonder if SocketSever is the bottleneck here?

