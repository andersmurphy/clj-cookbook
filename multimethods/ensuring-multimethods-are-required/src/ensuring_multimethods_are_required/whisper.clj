(ns ensuring-multimethods-are-required.whisper
  (:require [ensuring-multimethods-are-required.printer :as p]
            [clojure.string :as str]))

(defmethod p/print :whisper [{:keys [text]}]
  (println (str/lower-case text)))
