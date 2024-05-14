(ns server.core
  (:refer-clojure :exclude [pmap])
  (:import
   (java.lang ScopedValue)
   (java.util.function Supplier)
   (java.util.concurrent
     ExecutorService
     Executors
     Callable
     StructuredTaskScope
     StructuredTaskScope$Subtask
     StructuredTaskScope$ShutdownOnFailure
     StructuredTaskScope$ShutdownOnSuccess)))

(comment ;; pmap without structured concurrency
  (defonce executor
    (Executors/newVirtualThreadPerTaskExecutor))

  (defn pmap [f coll]
    (->> (mapv (fn [x] (ExecutorService/.submit executor
                         ;; More than one matching method found: submit
                         ;; So we need to type hint Callable
                         ^Callable (fn [] (f x))))
           coll)
      (mapv deref)))

  (pmap (fn [x]
          (let [result (inc x)]
            (Thread/sleep 50) ;; simulate some io
            (print (str "complete " result "\n"))
            result))
    [1 2 3 4 5 6])

  (pmap (fn [x]
          (let [result (inc x)]
            (Thread/sleep 50) ;; simulate some io
            (print (str "complete " result "\n"))
            result))
    [1 2 "3" 4 5 6])
  )

(comment ;; pmap with structured concurrency ShutdownOnFailure
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

  (pmap (fn [x]
          (let [result (inc x)]
            (Thread/sleep 50)
            (print (str "complete " result "\n"))
            result))
    [1 2 3 4 5 6])

  (pmap (fn [x]
          (let [result (inc x)]
            (Thread/sleep 50)
            (print (str "complete " result "\n"))
            result))
    [1 2 "3" 4 5 6])
  )

(comment ;; alts with structured concurrency ShutdownOnSuccess
  (defn alts [f coll]
    (with-open [scope (StructuredTaskScope$ShutdownOnSuccess/new)]
      (run! (fn [x]
              (StructuredTaskScope/.fork scope (fn [] (f x))))
        coll)
      ;; Throws if none of the subtasks completed successfully
      (.. scope join result)))

  (alts (fn [x]
          (let [result (inc x)]
            (Thread/sleep 100)
            (print (str "complete " result "\n"))
            result))
    [1 2 3 4 5 6])

  (alts (fn [x]
          (let [result (inc x)]
            (Thread/sleep 100)
            (print (str "complete " result "\n"))
            result))
    [1 2 "3" 4 5 6])
  )

(comment ;; binding conveyance with bound-fn*
  (def ^:dynamic *inc-amount* nil)

  (binding [*inc-amount* 3]
    (pmap (fn [x]
            (let [result (+ x *inc-amount*)]
              (Thread/sleep 50)
              (print (str "complete " result "\n"))
              result))
      [1 2 3 4 5 6]))

  (binding [*inc-amount* 3]
    (pmap (bound-fn*
            (fn [x]
              (let [result (+ x *inc-amount*)]
                (Thread/sleep 50)
                (print (str "complete " result "\n"))
                result)))
      [1 2 3 4 5 6]))
  )

(comment ;; Scoped Values
  (def scoped-inc-amount (ScopedValue/newInstance))

  ;; Single scoped value
  (ScopedValue/getWhere scoped-inc-amount 3
    (delay ;; https://clojure.atlassian.net/browse/CLJ-2792
      (pmap (fn [x]
              (let [result (+ x (ScopedValue/.get scoped-inc-amount))]
                (Thread/sleep 50)
                (print (str "complete " result "\n"))
                result))
        [1 2 3 4 5 6])))

  ;; pre CLJ-2792
  (ScopedValue/getWhere scoped-inc-amount 3
    (reify Supplier
      (get [_]
        (pmap (fn [x]
                (let [result (+ x (ScopedValue/.get scoped-inc-amount))]
                  (Thread/sleep 50)
                  (print (str "complete " result "\n"))
                  result))
          [1 2 3 4 5 6]))))

  (def scoped-dec-amount (ScopedValue/newInstance))

  ;; Multiple scoped values
  (.. (ScopedValue/where scoped-inc-amount 3)
    (ScopedValue/where scoped-dec-amount -2)
    (ScopedValue/get
      (delay
        (pmap (fn [x]
                (let [result (+ x
                               (ScopedValue/.get scoped-inc-amount)
                               (ScopedValue/.get scoped-dec-amount))]
                  (Thread/sleep 50)
                  (print (str "complete " result "\n"))
                  result))
          [1 2 3 4 5 6]))))

  ;; Convenience macro
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

  (scoped-binding [scoped-inc-amount  3
                   scoped-dec-amount -2]
    (pmap (fn [x]
            (let [result (+ x
                           (ScopedValue/.get scoped-inc-amount)
                           (ScopedValue/.get scoped-dec-amount))]
              (Thread/sleep 50)
              (print (str "complete " result "\n"))
              result))
      [1 2 3 4 5 6]))

  )
