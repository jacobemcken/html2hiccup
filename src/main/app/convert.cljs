(ns app.convert
  (:require [app.hickoy :as hickoy]
            [clojure.string :as str]
            [clojure.pprint :refer [pprint]]))

(def default-html
  "<div>
  <span>Hello World!</span>
  <!-- Can handle comments -->
  <a href=\"https://www.clojure.org\">Clojure webiste</a>
</div>")

(defn unnecessary-element
  [element]
  (or (and (coll? element)
           (empty? element))
      (and (string? element)
           (str/blank? element))))

(defn compact-data
  [d]
  (clojure.walk/prewalk (fn [x]
                          (cond
                            (or (map? x) (and (vector? x) (not (map-entry? x))))
                            (->> x (remove unnecessary-element) (into (empty x)))

                            (list? x)
                            (->> x (remove unnecessary-element))

                            :else x)) d))

(defn pp
  [x]
  (with-out-str (clojure.pprint/pprint x)))

(defn html->hiccup
  [html-str]
  (->> html-str
       (hickoy/parse-fragment)
       (hickoy/as-hiccup)
       (compact-data)
       (remove str/blank?)
       (map pp)
       (str/join)
       (str/trimr)))

