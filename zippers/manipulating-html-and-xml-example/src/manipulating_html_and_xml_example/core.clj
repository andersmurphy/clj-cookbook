(ns manipulating-html-and-xml-example.core
  (:require [hiccup.core :as hiccup]
            [hickory.core :as hick]
            [clojure.xml :as xml]
            [hickory.zip :as hick-zip]
            [clojure.zip :as zip]))

(def xml-feed (xml/parse "https://andersmurphy.com/feed.xml"))

(defn xml-feed->hiccup [xml-feed]
  (->> (zip/xml-zip xml-feed)
       (iterate zip/next)
       (take-while (complement zip/end?))
       (map zip/node)
       (filter (fn [node] (and (associative? node)
                               (= (:tag node) :item))))
       (map :content)
       (map (fn [[{[title] :content}
                  {[date] :content}
                  {[link] :content}]]
              [:div
               [:h1 title]
               [:p date]
               [:a {:href link} link]]))))

(def html-page (slurp "https://andersmurphy.com/"))

(defn zip-select-first [loc tag pred]
  (when-not (zip/end? loc)
    (if (some
         (every-pred associative?
                     #(some-> % tag pred))
         (zip/node loc))
      loc
      (recur (zip/next loc) tag pred))))

(defn build-page []
  (let [content (xml-feed->hiccup xml-feed)]
    (spit "page.html"
          (-> html-page
              hick/parse
              hick/as-hiccup
              hick-zip/hiccup-zip
              (zip-select-first :class #(= % "content container"))
              (zip/replace [:div {:class "content container"} content])
              zip/root
              hiccup/html))))
