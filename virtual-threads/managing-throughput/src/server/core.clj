(ns server.core
  (:import
   (java.util.concurrent Executors Semaphore
     ExecutorCompletionService)))

(defonce executor
  (Executors/newVirtualThreadPerTaskExecutor))

(defonce sem
  ;; 2rec/s
  (Semaphore/new 2 true))

(defn rate-limited-sem-release [sem]
  ;; block until available
  (Semaphore/.acquire sem)
  ;; Create another virtual thread that will release this semaphore
  ;; to refill the bucked when the time is up.
  (Thread/startVirtualThread
    #(do (Thread/sleep 1000) ;; wait 1 second
         (Semaphore/.release sem))))

(defn upmap
  ([f coll]
   (upmap nil f coll))
  ([sem f coll]
   (let [cs (ExecutorCompletionService/new executor)]
     (Thread/startVirtualThread
       #(run!
          (fn [x]
            (when sem (rate-limited-sem-release sem))
            (ExecutorCompletionService/.submit cs (fn [] (f x)))) coll))
     (->> (repeatedly #(deref (ExecutorCompletionService/.take cs)))
       (take (count coll))))))

(comment
  (time (upmap inc [1 2 3 4 5 6]))

  (time
    (->> (upmap sem inc [1 2 3 4 5 6 8 9 10])
      (run! prn))))

