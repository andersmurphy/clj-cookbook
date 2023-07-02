(ns sqlite.db.application-defined-functions)

(gen-class
  :name sqlite.db.application-defined-functions.HelloWorld
  :prefix "hello-world-"
  :extends org.sqlite.Function
  :exposes-methods {result superResult})

(defn hello-world-xFunc [this]
  (.superResult this "hello, world!"))

(gen-class
  :name sqlite.db.application-defined-functions.RegexCapture
  :prefix "regex-capture-"
  :extends org.sqlite.Function
  :exposes-methods {result superResult
                    value_text superValueText})

(defn regex-capture-xFunc [this]
  (.superResult this
    (let [result (re-find
                   (re-pattern
                     (.superValueText  this 0))
                   (.superValueText  this 1))]
      (if (vector? result)
        (second result)
        result))))

(comment
  (compile 'sqlite.db.application-defined-functions))
