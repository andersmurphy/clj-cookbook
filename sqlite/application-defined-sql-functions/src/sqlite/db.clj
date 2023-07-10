(ns sqlite.db
  (:require
   [next.jdbc :as jdbc])
  (:import
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
  (compile 'sqlite.db.HelloWorld)
  (compile 'sqlite.db.RegexCapture)
  
  (let [my-datasource (jdbc/get-datasource
                        (merge
                          data-source-properties
                          {:jdbcUrl "jdbc:sqlite:db/database.db"}))]
    (with-open [conn (jdbc/get-connection my-datasource)]
      (Function/create
        conn
        "hello_world"
        (sqlite.db.HelloWorld.))
      (jdbc/execute! conn ["select hello_world()"])))

  (let [my-datasource (jdbc/get-datasource
                        (merge
                          data-source-properties
                          {:jdbcUrl "jdbc:sqlite:db/database.db"}))]
    (with-open [conn (jdbc/get-connection my-datasource)]
      (Function/create
        conn
        "regex_capture"
        (sqlite.db.RegexCapture.))
      (jdbc/execute! conn
        ["select regex_capture(?, 'Hello, world!')"
         ", (world)!"])))
  
  (compile 'sqlite.db.application-defined-functions)

  (let [my-datasource (jdbc/get-datasource
                        (merge
                          data-source-properties
                          {:jdbcUrl "jdbc:sqlite:db/database.db"}))]
    (with-open [conn (jdbc/get-connection my-datasource)]
      (Function/create
        conn
        "hello_world"
        (sqlite.db.application-defined-functions.HelloWorld.))
      (jdbc/execute! conn ["select hello_world()"])))

  (let [my-datasource (jdbc/get-datasource
                        (merge
                          data-source-properties
                          {:jdbcUrl "jdbc:sqlite:db/database.db"}))]
    (with-open [conn (jdbc/get-connection my-datasource)]
      (Function/create
        conn
        "regex_capture"
        (sqlite.db.application-defined-functions.RegexCapture.))
      (jdbc/execute! conn
        ["select regex_capture(?, 'Hello, world!')"
         ", (world)!"])))
  
  )


