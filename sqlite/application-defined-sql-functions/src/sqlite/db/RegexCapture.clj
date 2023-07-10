(ns sqlite.db.RegexCapture
  (:gen-class
   :extends org.sqlite.Function
   :exposes-methods {result     superResult
                     value_text superValueText}))

(defn -xFunc [this]
  (.superResult this
    (let [result (re-find
                   (re-pattern
                     (.superValueText  this 0))
                   (.superValueText  this 1))]
      (if (vector? result)
        (second result)
        result))))
