(ns ensuring-multimethods-are-required.core
  (:require
   [ensuring-multimethods-are-required.whisper]
   [ensuring-multimethods-are-required.shout]
   [ensuring-multimethods-are-required.printer :as p]))

(let [loaded-methods (-> p/print methods keys set)
      expected-methods #{:default :shout :whisper}]
  (assert (= expected-methods loaded-methods)
          (str expected-methods " =/= " loaded-methods)))

(run! p/print [{:text "Hello"}
               {:type :shout :text "Hello"}
               {:type :whisper :text "Hello"}])
