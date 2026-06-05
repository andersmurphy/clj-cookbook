(ns app.core
  (:require [sqlite4clj.core :as d]
            [clj-async-profiler.core :as prof])
  (:import (io.github.robsonkades.uuidv7 UUIDv7)
           (java.nio ByteBuffer)
           (java.util UUID)))

(set! *warn-on-reflection* true)

(defn random-uuid4-bytes []
  (let [uuid (random-uuid)
        bb   (ByteBuffer/wrap (byte-array 16))]
    (doto bb
      (ByteBuffer/.putLong (UUID/.getMostSignificantBits uuid))
      (ByteBuffer/.putLong (UUID/.getLeastSignificantBits uuid)))
    (ByteBuffer/.array bb)))

(defn random-uuid7-bytes []
  (let [uuid (UUIDv7/randomUUID)
        bb   (ByteBuffer/wrap (byte-array 16))]
    (doto bb
      (ByteBuffer/.putLong (UUID/.getMostSignificantBits uuid))
      (ByteBuffer/.putLong (UUID/.getLeastSignificantBits uuid)))
    (ByteBuffer/.array bb)))

(defn encode-data [data]
  (String/.getBytes (pr-str data)))

(defonce db
  (d/init-db! "db/db.db"
    {:pool-size 4 :pragma {:synchronous "FULL"}}))

(def writer (:writer db))

(def data (encode-data
            {:type    "article"
             :message "Clojure <3 Datastar!"}))

(comment
  (do
    (d/q writer
      ["DROP TABLE IF EXISTS event"])
    (d/q writer ["PRAGMA wal_checkpoint(TRUNCATE)"])
    (d/q writer
      ["CREATE TABLE IF NOT EXISTS event(id INT PRIMARY KEY, data BLOB)"]))
  
  (dotimes [_ 10]
    (time
      (d/with-write-tx [db writer]
        (dotimes [_ 1000000]
          (d/q db ["INSERT INTO event (data) values (?)" data])))))

  (d/q (:reader db) ["SELECT count(*) from event"])

  )

(comment
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
                   (random-uuid4-bytes) data])))))

  (d/q (:reader db) ["SELECT count(*) from event"])

  )

(comment
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
                   (random-uuid7-bytes) data])))))

  (d/q (:reader db) ["SELECT count(*) from event"])

  )



(comment ;; Profiling

  (prof/generate-diffgraph 1 2 {})
 
  
  (prof/serve-ui 7777)
  ;; (clojure.java.browse/browse-url "http://localhost:7777/")
  )
