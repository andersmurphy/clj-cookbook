(ns sendgrid-example.core
  (:require [clj-http.client :as http]
            [clojure.string :as str]))

(def data [{:name "Bob" :age 27 :favourite-food "bagels"}
           {:name "Sarah" :age 23 :favourite-food "apples"}
           {:name "John" :age 41 :favourite-food "pasta"}])

(defn escape-csv-value [value]
  (str "\"" value "\""))

(defn row->csv-row [row]
  (->> (map escape-csv-value row)
       (str/join ",")))

(defn ms->csv-string [ms]
  (let [columns (keys (first ms))
        headers (map name columns)
        rows    (map #(map % columns) ms)]
    (->> (into [headers] rows)
         (map row->csv-row)
         (str/join "\n"))))

(defn encode-string-to-base64 [string]
  (.encodeToString (java.util.Base64/getEncoder) (.getBytes string)))

(defn send-email-with-csv [to-email csv-string]
  (http/post
   "https://api.sendgrid.com/v3/mail/send"
   {:headers      {:authorization
                   (str "Bearer " (System/getenv "SENGRID_API_KEY"))}
    :content-type :json
    :form-params
    {:personalizations [{:to      [{:email to-email}]
                         :subject "Hello, World!"}]
     :from             {:email "from_address@exampl.com"}
     :content          [{:type  "text/plain"
                         :value "Hello, World!"}]
     :attachments
     [{:filename "helloworld.csv"
       :content  (encode-string-to-base64 csv-string)}]}}))

(comment
  (->> data
       ms->csv-string
       (send-email-with-csv "john@example.com")))
