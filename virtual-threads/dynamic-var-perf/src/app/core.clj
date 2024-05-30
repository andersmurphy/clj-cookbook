(ns app.core
  (:require [criterium.core :as crit])
  (:refer-clojure :exclude [pmap])
  (:import
   (java.lang ScopedValue)
   (java.util.concurrent
     StructuredTaskScope
     StructuredTaskScope$Subtask
     StructuredTaskScope$ShutdownOnFailure)))

(defn pmap [f coll]
  (with-open [scope (StructuredTaskScope$ShutdownOnFailure/new)]
    (let [r (mapv (fn [x]
                    (StructuredTaskScope/.fork scope
                      (fn [] (f x))))
              coll)]
      ;; join subtasks and propagate errors
      (.. scope join throwIfFailed)
      ;; fork returns a Subtask/Supplier not a future
      (mapv StructuredTaskScope$Subtask/.get r))))

(defmacro scoped-binding [bindings & body]
  (assert (vector? bindings)
    "a vector for its binding")
  (assert (even? (count bindings))
    "an even number of forms in binding vector")
  `(.. ~@(->> (partition 2 bindings)
           (map (fn [[k v]]
                  (assert (-> k resolve deref type (= ScopedValue))
                    (str k " is not a ScopedValue"))
                  `(ScopedValue/where ~k ~v))))
     (ScopedValue/get (delay ~@body))))

(comment
  (def ^:dynamic *context* nil)

  (def context-data
    {:increase 1
     :colors   {:red 1 :blue 2 :green 3}
     :a        "A bunch of context stuff"
     :b        "B bunch of context stuff"
     :c        "C bunch of context stuff"})

  (crit/quick-bench
    (binding [*context* context-data]
      (pmap (bound-fn*
              (fn [x]
                (let [result (+ x (:increase *context*))]
                  result)))
        (repeat 500000 1))))

  ;; Evaluation count : 6 in 6 samples of 1 calls.
  ;;            Execution time mean : 1.196520 sec
  ;;   Execution time std-deviation : 41.818110 ms
  ;;  Execution time lower quantile : 1.143932 sec ( 2.5%)
  ;;  Execution time upper quantile : 1.243698 sec (97.5%)
  ;;                  Overhead used : 1.845282 ns

  (def scoped-context (ScopedValue/newInstance))

  (crit/quick-bench
    (scoped-binding [scoped-context  context-data]
      (pmap (fn [x]
              (let [result (+ x
                             (:increase (ScopedValue/.get scoped-context)))]
                result))
        (repeat 500000 1))))

  ;; Evaluation count : 6 in 6 samples of 1 calls.
  ;;            Execution time mean : 197.549698 ms
  ;;   Execution time std-deviation : 24.230133 ms
  ;;  Execution time lower quantile : 172.035517 ms ( 2.5%)
  ;;  Execution time upper quantile : 220.525422 ms (97.5%)
  ;;                  Overhead used : 1.845282 ns

  (crit/quick-bench
    (let [context context-data]
      (pmap (fn [x]
              (let [result (+ x
                             (:increase context))]
                result))
        (repeat 500000 1))))

  ;; Evaluation count : 6 in 6 samples of 1 calls.
  ;;            Execution time mean : 189.313904 ms
  ;;   Execution time std-deviation : 22.768815 ms
  ;;  Execution time lower quantile : 165.292587 ms ( 2.5%)
  ;;  Execution time upper quantile : 215.966794 ms (97.5%)
  ;;                  Overhead used : 1.845282 ns

  )

