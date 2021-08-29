(ns rewrite-clj.core
  (:require [rewrite-clj.zip :as z]
            [clojure.string :as str]
            [clojure.java.io :as io]))

(defn kebab-case->camelCase
  [k]
  (let [words (str/split (name k) #"-")]
    (->> (map str/capitalize (rest words))
         (apply str (first words))
         keyword)))

(defn skip-form [zloc]
  (or (z/right zloc)
      (if-not (z/up zloc)
        [(z/node zloc) :end]
        (recur (z/up zloc)))))

(defn camel-case-snake-case-keys
  [{:keys [ignored-forms ignored-keys]} code]
  (let [ignored-forms (conj ignored-forms
                            'camel-case-snake-case-keys)]
    (loop [zloc (z/of-string code)]
      (if (z/end? zloc)
        (z/root-string zloc)
        (-> (let [sexpr (z/sexpr zloc)]
              (cond
                (and (or (list? sexpr) (vector? sexpr))
                     (get ignored-forms (first sexpr)))
                (skip-form zloc),
                (and (keyword? sexpr)
                     (not (namespace sexpr))
                     (not (get ignored-keys sexpr)))
                (-> (z/edit zloc kebab-case->camelCase)
                    z/next),
                :else (z/next zloc)))
            recur)))))

(defn get-clj-files
  [folder-path]
  (->> (io/file folder-path)
       file-seq
       (filter #(.isFile %))
       (filter #(str/ends-with? (str %) ".clj"))))

(comment
  (->> (get-clj-files "/Users/andersmurphy/projects/clj-cookbook")
       (run! (fn [file]
               (->> (slurp file)
                    (camel-case-snake-case-keys
                     {:ignored-keys
                      #{:content-type
                        :form-params}
                      :ignored-forms
                      #{'env}})
                    (spit file))))))

(comment
  (env :foo-bar)
  (foo (env {:foo-bar 23})
       (env {:foo-bar 23}))
  (env :foo-bar)
  (env :foo-var :bar-var)
  (-> :foo-car)
  (env :foo-var))

;; Exclude files
