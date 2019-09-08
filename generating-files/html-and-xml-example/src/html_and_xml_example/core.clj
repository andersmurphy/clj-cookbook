(ns html-and-xml-example.core
  (:require [hiccup.core :as html]
            [clojure.data.xml :as xml]))

(def site-url "https://andersmurphy.com")

(defn generate-404-html []
  (html/html [:html
              [:body
               [:h1 {:class "post-title"} "404: Page not found"]
               [:p "Sorry, we've misplaced that URL or it's
                 pointing to something that doesn't exist."
                [:a {:href site-url} "Head back home"]
                " to try finding it again."]]]))

(defn write-404! [html]
  (let [path-name "404.html"]
    (spit path-name html)))

(comment (-> (generate-404-html)
             write-404!))

(def site-title "Site Title")
(def site-rss (str site-url "/feed.xml"))
(def site-description "Site Description")

(defn generate-rss-xml [posts]
  (xml/sexp-as-element
   [:rss
    {:version    "2.0"
     :xmlns:atom "https://www.w3.org/2005/Atom"
     :xmlns:dc   "https://purl.org/dc/elements/1.1/"}
    [:channel
     [:title site-title]
     [:description site-description]
     [:link site-url]
     [:atom:link
      {:href site-rss :rel "self" :type "application/rss+xml"}]
     (map (fn [{:keys [post-name date post-path-name]}]
            (let [post-url (str site-url "/" post-path-name)]
              [:item
               [:title post-name]
               [:pubDate date]
               [:link post-url]
               [:guid {:isPermaLink "true"} post-url]]))
          posts)]]))

(def posts [{:post-name      "Foo"
             :post-path-name "foo"
             :date           "Fri, 6 Sep 2019 00:00:00 GMT"}
            {:post-name      "Bar"
             :post-path-name "bar"
             :date           "Sat, 7 Sep 2019 00:00:00 GMT"}
            {:post-name      "Baz"
             :post-path-name "baz"
             :date           "Sun, 8 Sep 2019 00:00:00 GMT"}])

(defn write-rss! [xml]
  (with-open [out-file (java.io.FileWriter. "feed.xml")]
    (xml/emit xml out-file)))

(comment (-> (generate-rss-xml posts)
             write-rss!))
