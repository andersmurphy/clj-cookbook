(ns app.core
  (:require [sqlite4clj.core :as d]
            [clj-async-profiler.core :as prof]
            [clojure.edn :as edn])
  (:import (java.security SecureRandom)
           (java.nio ByteBuffer)))

(set! *warn-on-reflection* true)

(defn encode-data [data]
  (String/.getBytes (pr-str data)))

(defonce db
  (d/init-db! "db/db.db"
    {:pool-size 4 :pragma {:synchronous "FULL"}}))

(def writer (:writer db))

(def data (encode-data
            {:type    "article"
             :message "Clojure <3 Datastar!"}))

(def ^SecureRandom secure-random
  (SecureRandom/new))

(defn random-unguessable-id []
  (let [buffer (byte-array 20)]
    (.nextBytes secure-random buffer)
    buffer))

(defn bytes->long [^bytes bytes]
  (let [bb (ByteBuffer/allocate (count bytes))]
    (ByteBuffer/.put bb bytes)
    (ByteBuffer/.getLong bb 0)))

(defn byte-compare
  "Compares the first 8 most significant bytes of a byte array.
  Big Endian (matches SQLites blob sort)."
  [a b]
  (Long/compareUnsigned
    (bytes->long a)
    (bytes->long b)))

(comment
  (type (bytes->long (random-unguessable-id))))

(comment ;; random id without pre-sort
  (do
    (d/q writer
      ["DROP TABLE IF EXISTS event"])
    (d/q writer ["PRAGMA wal_checkpoint(TRUNCATE)"])
    (d/q writer
      ["CREATE TABLE IF NOT EXISTS event(id BLOB PRIMARY KEY, data BLOB) WITHOUT ROWID"]))

  (dotimes [_ 10]
    (time
      (d/with-write-tx [db writer]
        (dotimes [_ 1000000]
          (d/q db ["INSERT INTO event (id, data) values (?, ?)"
                   (random-unguessable-id) data])))))

  (d/q (:reader db) ["SELECT count(*) from event"])

  )

(comment ;; random-id with pre-sort
  (do
    (d/q writer
      ["DROP TABLE IF EXISTS event"])
    (d/q writer ["PRAGMA wal_checkpoint(TRUNCATE)"])
    (d/q writer
      ["CREATE TABLE IF NOT EXISTS event(id BLOB PRIMARY KEY, data BLOB) WITHOUT ROWID"]))

  (dotimes [_ 10]
    (time
      (d/with-write-tx [db writer]
        (->> (repeatedly 1000000 random-unguessable-id)
          (sort byte-compare)
          (run!
            (fn [id]
              (d/q db ["INSERT INTO event (id, data) values (?, ?)"
                       id data])))))))

  (d/q (:reader db) ["SELECT count(*) from event"])

  )

(comment ;; Profiling


  (prof/serve-ui 7777)
  ;; (clojure.java.browse/browse-url "http://localhost:7777/")
  )
