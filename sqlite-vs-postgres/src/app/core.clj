(ns app.core
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql :as jdbc-sql ]
            [next.jdbc.connection :as connection]
            [sqlite4clj.core :as d]
            [sqlite4clj.batch :as b])
  (:import (com.zaxxer.hikari HikariDataSource)
           (java.util.concurrent Executors ExecutorService)))

(defonce pg-db
  (jdbc/with-options
    (connection/->pool
      HikariDataSource
      {:dbtype          "postgres"
       :dbname          "thedb"
       :username        (System/getProperty "user.name")
       :password        ""
       :minimumIdle     8
       :maximumPoolSize 8})
    {}))

(defn rand-pareto [r p]
  (let [a (/ (Math/log (- 1.0 p)) (Math/log p))
        x (rand)
        y (/ (- (+ (Math/pow x a) 1.0)
               (Math/pow (- 1.0 x) (/ 1.0 a)))
            2.0)]
    (long (* r y))))

(defn pareto-user []
  (rand-pareto (* 1000 1000 1000) 0.9995))

(defonce lite-read-pool (Executors/newFixedThreadPool 7))
(defonce lite-write-pool (Executors/newFixedThreadPool 1))

(defonce lite-db
  (d/init-db! "database.db"
    {:pool-size 8}))

(defmacro on-pool! [pool & body]
  `(ExecutorService/.submit ~pool ^Callable
     (fn [] ~@body)))

(defn batch-fn [db batch]
  @(on-pool! lite-write-pool
     (d/with-write-tx [tx db]
       (run! (fn [thunk] (thunk tx)) batch))
     ;; (d/q (lite-db :writer) ["PRAGMA wal_checkpoint(PASSIVE)"])
     ))

(defonce tx!
  (b/async-batcher-init! lite-db
    {:batch-fn #'batch-fn}))

;; Make futures use virtual threads
(set-agent-send-executor!
  (Executors/newVirtualThreadPerTaskExecutor))
(set-agent-send-off-executor!
  (Executors/newVirtualThreadPerTaskExecutor))

(defn credit-random-account []
  ["UPDATE account SET balance = balance - 1.00
    WHERE id = ?;" (pareto-user)])

(defn debit-random-account []
  ["UPDATE account SET balance = balance + 1.00
    WHERE id = ?;" (pareto-user)])

(defmacro tx-per-second [n & body]
  `(let [ids#   (range 0 ~n)
         start# (. System (nanoTime))]
     (->> ids#
       ;; Futures are using virtual threads so blocking is not slow
       (mapv (fn [_#] (future ~@body)))
       (run! deref))
     (int (/ ~n (/ (double (- (. System (nanoTime)) start#)) 1000000000.0)))))

(comment
  ;; 785152000
  ;; createdb thedb
  (jdbc/execute! pg-db
    ["CREATE TABLE IF NOT EXISTS account(id INT PRIMARY KEY, balance INT)"])
  (->> (range 0 (* 1000 1000 1000))
    (partition-all 32000)
    (run!
      (fn [batch]
        (jdbc-sql/insert-multi! pg-db :account
          (mapv (fn [id] {:id id :balance 1000000000}) batch)))))


  (d/q (lite-db :writer)
    ["CREATE TABLE IF NOT EXISTS account(id PRIMARY KEY, balance INT)"])
  (->> (range 0 (* 1000 1000 1000))
    (partition-all 100000)
    (run!
      (fn [batch]
        (d/with-write-tx [tx (lite-db :writer)]
          (run!
            (fn [id]
              (d/q tx
                ["INSERT INTO account(id, balance) VALUES (?,?)"
                 id 1000000000]))
            batch)))))
  )

(comment
  ;; Pareto distribution

  ;; 15625 -> 1000000
  ;; (float (* (/ 15625 1000000) 100))
  ;; 1% cache

  (tx-per-second 100000
    (d/with-write-tx [tx (lite-db :writer)]
      (d/q tx (credit-random-account))
      (d/q tx (debit-random-account))))

  (tx-per-second 1000000
    @(tx!
       (fn [tx]
         (d/q tx (credit-random-account))
         (d/q tx (debit-random-account)))))
  ;; 169599
  ;; 256724

  (tx-per-second 100000
    (jdbc/with-transaction [tx pg-db]
      (jdbc/execute! tx (credit-random-account))
      (jdbc/execute! tx (debit-random-account))))

  (tx-per-second 10000
    (jdbc/with-transaction [tx pg-db]
      (jdbc/execute! tx (credit-random-account))
      (Thread/sleep 5)
      (jdbc/execute! tx (debit-random-account))))

  (tx-per-second 10000
    (jdbc/with-transaction [tx pg-db]
      (jdbc/execute! tx (credit-random-account))
      (Thread/sleep 10)
      (jdbc/execute! tx (debit-random-account))))

  (tx-per-second 10000
    (loop []
      (let [result
            (try
              (jdbc/with-transaction [tx pg-db {:isolation :serializable}]
                (jdbc/execute! tx (credit-random-account))
                (Thread/sleep 10)
                (jdbc/execute! tx  (debit-random-account)))
              (catch Exception _ nil))]
        (when-not result (recur)))))

  (tx-per-second 10000
    (loop []
      (let [result
            (try
              (jdbc/with-transaction [tx pg-db {:isolation :serializable}]
                (jdbc/execute! tx (credit-random-account))
                (Thread/sleep 10)
                (jdbc/execute! tx  (debit-random-account))
                (Thread/sleep 10)
                (jdbc/execute! tx  (debit-random-account)))
              (catch Exception _ nil))]
        (when-not result (recur)))))

  (tx-per-second 200000
    (on-pool! lite-read-pool
      (d/q (lite-db :reader)
        ["select * from account where id = ? limit 1" (pareto-user)]))
    (on-pool! lite-read-pool
      (d/q (lite-db :reader)
        ["select * from account where id = ? limit 1" (pareto-user)]))
    (on-pool! lite-read-pool
      (d/q (lite-db :reader)
        ["select * from account where id = ? limit 1" (pareto-user)]))
    @(tx!
       (fn  [tx]
         (d/q tx ["SAVEPOINT inner_tx"])
         (try
           (d/q tx (credit-random-account))
           (d/q tx (debit-random-account))
           (catch Throwable _
             (d/q tx ["ROLLBACK inner_tx"])))
         (d/q tx ["RELEASE inner_tx"]))))

  (tx-per-second 1000000
    @(tx!
       (fn  [tx]
         (d/q tx ["SAVEPOINT inner_tx"])
         (try
           (d/q tx (credit-random-account))
           (d/q tx (debit-random-account))
           (catch Throwable _
             (d/q tx ["ROLLBACK inner_tx"])))
         (d/q tx ["RELEASE inner_tx"]))))
  ;; 129255
  ;; 178020

  (jdbc/execute! pg-db ["select count(*) from account"])
  (d/q (lite-db :reader) ["select count(*) from account"])

  )

(comment
  ;; Free up space (slow)
  (time (jdbc/execute! pg-db ["VACUUM"]))
  (time (d/q (lite-db :writer) ["VACUUM"]))
  (time (d/optimize-db (lite-db :writer)))
  (time (d/optimize-db (lite-db :reader)))
  (time (d/q (lite-db :writer) ["PRAGMA analysis_limit=1000; ANALYZE"]))

  (+ 3 4)

  ;; Checkpoint the WAL
  (d/q (lite-db :writer) ["PRAGMA wal_checkpoint(PASSIVE)"])
  (d/q (lite-db :writer) ["PRAGMA wal_checkpoint(TRUNCATE)"]))


