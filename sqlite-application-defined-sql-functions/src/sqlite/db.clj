(ns sqlite.db
  (:require
   [next.jdbc :as jdbc])
  (:import
   (sqlite.db HelloWorld)
   (org.sqlite Function)))

(def data-source-properties
  ;; For more info on sqlite-jdbc properties
  ;; https://github.com/xerial/sqlite-jdbc/blob/master/src/main/java/org/sqlite/SQLiteConfig.java#L376
  {;; https://phiresky.github.io/blog/2020/sqlite-performance-tuning/
   ;; https://www.sqlite.org/pragma.html#pragma_cache_size
   ;; (/ 6.4e+7 4096) -> 15625
   ;; (* 15625 4096 1e-6) -> 64MB
   :cache_size            15625
   :page_size             4096
   ;; https://www.sqlite.org/wal.html
   ;; https://www.sqlite.org/pragma.html#pragma_journal_mode
   :journal_mode          "WAL"
   ;; https://www.sqlite.org/pragma.html#pragma_synchronous
   :synchronous           "NORMAL"
   :temp_store            "memory"
   :foreign_keys          true})

(comment
  
  (let [my-datasource (jdbc/get-datasource
                        (merge
                          data-source-properties
                          {:jdbcUrl "jdbc:sqlite:db/database.db"}))]
    (with-open [conn (jdbc/get-connection my-datasource)]
      (Function/create
        conn
        "hello_world"
        (HelloWorld.))
      (jdbc/execute! conn ["select hello_world()"])
      ))
  )


