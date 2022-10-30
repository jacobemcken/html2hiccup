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
  "The HTML to Hiccup transformation produces undesired elements, i.e.:
   <div>   <span>hello</span>   </div>
   would produce:
   [:div {} \"   \" [:span {} \"hello\"] \"   \"]
   Notice the empty attribute map and empty string (formating leftovers) on
   the string 'hello'."
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

                            (string? x) ; trim whitespace usually caused by pretty formated HTML: <span>\n   Hello\n</span>
                            (str/trim x)

                            :else x)) d))

(defn pp
  [x]
  ;; "miser-width" avoids some unnecessary linebreaks
  (binding [clojure.pprint/*print-miser-width* 2] ; https://clojuredocs.org/clojure.pprint/*print-miser-width*
    (with-out-str (clojure.pprint/pprint x))))
;; for a saner "pretty printer", I think a custom printer is needed that can handle:
;; vector, keyword, map, string and "comments" (list and comment-symbol)
;; https://github.com/clojure/clojure/blob/b1b88dd25373a86e41310a525a21b497799dbbf2/src/clj/clojure/core_print.clj#L225
;; https://github.com/clojure/clojurescript/blob/180d789ea6dd41c57684b930fd6f3167dbcea614/src/main/clojure/cljs/core.cljc#L1892-L1898

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

