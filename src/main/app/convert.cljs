(ns app.convert
  (:require [clojure.string :as str]
            [clojure.pprint :refer [pprint]]
            [hickory.utils :as utils]
            [hickory.core :as hickory]))

(extend-protocol hickory/HiccupRepresentable
  object
  (as-hiccup [this] (condp = (aget this "nodeType")
                      hickory/Attribute [(let [attr-name (aget this "name")]
                                           (if (= "viewBox" attr-name)
                                             (keyword attr-name)
                                             (utils/lower-case-keyword attr-name)))
                                         (aget this "value")]
                      hickory/Comment (list 'comment (str/trim (aget this "data")))
                      hickory/Document (map hickory/as-hiccup (aget this "childNodes"))
                      hickory/DocumentType (hickory/format-doctype this)
                      hickory/Element (let [tag (utils/lower-case-keyword (aget this "tagName"))]
                                        (into [] (concat [tag
                                                          (into {} (map hickory/as-hiccup (aget this "attributes")))]
                                                         (if (utils/unescapable-content tag)
                                                           (map #(aget % "wholeText") (aget this "childNodes"))
                                                           (map hickory/as-hiccup (aget this "childNodes"))))))
                      hickory/Text (utils/html-escape (aget this "wholeText")))))

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
       (hickory/parse-fragment)
       (map #(-> %
                 hickory/as-hiccup
                 compact-data))
       (remove str/blank?)
       (map pp)
       (str/join)
       (str/trimr)))
