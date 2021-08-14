(ns rewrite-clj.core
  (:require [rewrite-clj.zip :as z]
            [clojure.string :as str]
            [clojure.java.io :as io]))

(defn kebab-case->camelCase [k]
  (let [words (str/split (name k) #"-")]
    (->> (map str/capitalize (rest words))
         (apply str (first words))
         keyword)))

(defn camel-case-snake-case-keys [code]
  (loop [zloc (z/of-string code)]
    (if (z/end? zloc)
      (z/root-string zloc)
      (let [zloc (z/next zloc)]
        (if (and (= :token (z/tag zloc))
                 (keyword? (z/sexpr zloc)))
          (recur (z/edit zloc kebab-case->camelCase))
          (recur zloc))))))

(defn get-clj-files
  [folder-path]
  (->> (io/file folder-path)
       file-seq
       (filter #(.isFile %))
       (filter #(str/ends-with? (str %) ".clj"))))

(comment
  (->> (get-clj-files "/Users/andersmurphy/projects/clj-cookbook")
       (run! (fn [file] (->> (slurp file)
                             camel-case-snake-case-keys
                             (spit file))))))
;; Add exclusion list
;; Ignore namespaced keywords
