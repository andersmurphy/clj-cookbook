(ns server.core
  (:require [clojure.walk :as walk]
            [malli.core :as m]
            [clojure.string :as str]
            [hickory.core :as hick]
            [lambdaisland.regal :refer [regex]]))

(defn find-last-tag-match [tags hiccup]
  (let [branch?  sequential?
        children (fn [children] (filter branch? children))]
    (->> (tree-seq branch? children hiccup)
      (filterv (fn [[t :as el]] (when (tags t) el)))
      peek)))

(def comment-regex ;; #"<!--.*-->"
  (regex [:cat "<!--" [:* :any] "-->"]))

(defn tags-to-remove [tags]
  [:or
   [:cat [:fn tags] [:* :any]]
   [:cat [:not [:fn #{:br}]] :any]
   [:and :string [:or [:fn str/blank?] [:re comment-regex]]]
   [:fn (fn [x] (= org.jsoup.nodes.TextNode (type x)))]])

(def tags-to-unwrap
  [:cat [:fn #{:div :span :article :main}] :any [:or [:* :any] :string]])

(defn remove-tags [tags hiccup]
  (let [remove-tag? (m/validator (tags-to-remove tags))
        unwrap-tag? (m/validator tags-to-unwrap)]
    (-> (walk/postwalk
          #(cond (and (sequential? %) (not (map-entry? %)))
                 (let [[_ _ f-child :as el]
                       (into [] (remove remove-tag?) %)]
                   (if (unwrap-tag? el) f-child el))
                 (map? %) (dissoc % :class :id :style :dir
                            :aria-label)
                 :else    %)
          hiccup))))

(comment
  (def html-data
    (slurp "https://clojure.org/reference/clojure_cli"))

  (->> html-data hick/parse hick/as-hiccup
    (find-last-tag-match #{:body :main})
    (remove-tags #{:head :script :noscript :style :nav :meta
                   :form :fieldset :object :embed :footer
                   :link :aside :iframe :input :textarea
                   :select :button :template})))
