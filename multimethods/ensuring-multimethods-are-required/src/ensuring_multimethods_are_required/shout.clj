(ns ensuring-multimethods-are-required.shout
  (:require [ensuring-multimethods-are-required.printer :as p]
            [clojure.string :as str]))

(defmethod p/print :shout [{:keys [text]}]
  (println (str/upper-case text)))
