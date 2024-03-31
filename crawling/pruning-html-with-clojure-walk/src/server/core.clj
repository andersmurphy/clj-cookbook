(ns server.core
  (:require [clojure.walk :as walk]
            [malli.core :as m]
            [hickory.core :as hick]
            [lambdaisland.regal :refer [regex]]))

(defn find-last-tag-match [tags hiccup]
  (let [branch?  sequential?
        children (fn [children] (filter branch? children))]
    (->> (tree-seq branch? children hiccup)
      (filterv (fn [[t :as el]] (when (tags t) el)))
      peek)))

(def blank-re
  ;; Malli uses re-find not re-matches so we need to specify start/end
  ;; or we will match on strings that contain whitespace and other content.
  ;; See: https://github.com/metosin/malli/issues/862
  [:cat :start [:* :whitespace] :end])
(def comment-re  [:cat "<!--" [:* :any] "-->"])
(def doc-type-re "<!DOCTYPE html>")

(def strings-to-remove
  (regex [:alt blank-re comment-re doc-type-re]))

(defn tags-to-remove [tags]
  [:or
   [:cat [:fn tags] [:* :any]]
   [:cat [:not [:fn #{:br}]] :any]
   [:and :string [:re strings-to-remove]]
   [:not [:or :keyword :string [:vector :any] :map]]])

(def tags-to-unwrap
  [:or [:cat [:fn #{:div :span :article :main}]
        :any [:or [:* :any] :string]]
   [:cat [:vector :any]]])

(defn remove-tags [tags hiccup]
  (let [remove-tag? (m/validator (tags-to-remove tags))
        unwrap-tag? (m/validator tags-to-unwrap)]
    (walk/postwalk
      #(cond (and (vector? %) (not (map-entry? %)))
             (let [el (into [] (remove remove-tag?) %)]
               (if (unwrap-tag? el) (peek el) el))
             (map? %) (dissoc % :class :id :style :dir
                        :aria-label)
             :else    %)
      (vec hiccup))))

(comment
  (def html-data
    (slurp "https://clojure.org/reference/clojure_cli"))

  (->> html-data hick/parse hick/as-hiccup
    (find-last-tag-match #{:body :main})
    (remove-tags #{:head :script :noscript :style :nav :meta
                   :form :fieldset :object :embed :footer
                   :link :aside :iframe :input :textarea
                   :select :button :template})))
