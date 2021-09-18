(ns ensuring-multimethods-are-required.printer
  (:refer-clojure :exclude [print]))

(defmulti print :type)

(defmethod print :default [{:keys [text]}] (println text))
