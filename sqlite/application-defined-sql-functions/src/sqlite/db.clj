(ns sqlite.db
  (:require
   [next.jdbc :as jdbc])
  (:import
   (org.sqlite Function)))

(comment  
  (compile 'sqlite.db.application-defined-functions)

  (let [my-datasource (jdbc/get-datasource
                        {:jdbcUrl "jdbc:sqlite:db/database.db"})]
    (with-open [conn (jdbc/get-connection my-datasource)]
      (Function/create
        conn
        "hello_world"
        (sqlite.db.application-defined-functions.HelloWorld.))
      (jdbc/execute! conn ["select hello_world()"])))

  (let [my-datasource (jdbc/get-datasource
                        {:jdbcUrl "jdbc:sqlite:db/database.db"})]
    (with-open [conn (jdbc/get-connection my-datasource)]
      (Function/create
        conn
        "regex_capture"
        (sqlite.db.application-defined-functions.RegexCapture.))
      (jdbc/execute! conn
        ["select regex_capture(?, 'Hello, world!')"
         ", (world)!"])))
  
  )


