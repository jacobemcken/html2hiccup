(ns app.hickoy
  "'Hickoy' is a slim standin for Hikory: https://github.com/clj-commons/hickory
   Code is mostly copied but a few changes were added:
   - Don't ignore initial HTML comments (when using fragments)
   - Convert HTML comments to Clojure comment blocks instead of strings (which Hiccup will render)
   - Support viewBox (camel case) attribute on SVG's"
  (:require [clojure.string :as str]
            [goog.string :as gstring]))

(defprotocol HiccupRepresentable
  "Objects that can be represented as Hiccup nodes implement this protocol in
   order to make the conversion."
  (as-hiccup [this]
    "Converts the node given into a hiccup-format data structure. The
     node must have an implementation of the HiccupRepresentable
     protocol; nodes created by parse or parse-fragment already do."))

(defn node-type
  [type]
  (aget js/Node (str type "_NODE")))

(def Attribute (node-type "ATTRIBUTE"))
(def Comment (node-type "COMMENT"))
(def Document (node-type "DOCUMENT"))
(def DocumentFragment (node-type "DOCUMENT_FRAGMENT")) ; non-Hickory
(def DocumentType (node-type "DOCUMENT_TYPE"))
(def Element (node-type "ELEMENT"))
(def Text (node-type "TEXT"))

(defn extend-type-with-seqable
  [t]
  (extend-type t
    ISeqable
    (-seq [array] (array-seq array))))

(extend-type-with-seqable js/NodeList)

(extend-type-with-seqable js/DocumentFragment) ; non-Hickory

(when (exists? js/NamedNodeMap)
  (extend-type-with-seqable js/NamedNodeMap))

(when (exists? js/MozNamedAttrMap) ;;NamedNodeMap has been renamed on modern gecko implementations (see https://developer.mozilla.org/en-US/docs/Web/API/NamedNodeMap)
  (extend-type-with-seqable js/MozNamedAttrMap))

(defn format-doctype
  [dt]
  (let [name (aget dt "name")
        publicId (aget dt "publicId")
        systemId (aget dt "systemId")]
    (if (not (empty? publicId))
      (gstring/format "<!DOCTYPE %s PUBLIC \"%s\" \"%s\">" name publicId systemId)
      (str "<!DOCTYPE " name ">"))))

(defn lower-case-keyword
  "Converts its string argument into a lowercase keyword."
  [s]
  (-> s str/lower-case keyword))

(def unescapable-content
  "Elements whose content should never have html-escape codes."
  #{:script :style})

(def camel-case-attrs
  #{"viewBox"
    "baseProfile"})

(extend-protocol HiccupRepresentable
  object
  (as-hiccup [this] (condp = (aget this "nodeType")
                      Attribute [(let [attr-name (aget this "name")]
                                   (if (contains? camel-case-attrs attr-name)   ; non-Hickory
                                     (keyword attr-name)
                                     (lower-case-keyword attr-name)))
                                 (aget this "value")]
                      Comment (list 'comment (str/trim (aget this "data")))     ; non-Hickory
                      Document (map as-hiccup (aget this "childNodes"))
                      DocumentFragment (map as-hiccup (aget this "childNodes")) ; non-Hickory
                      DocumentType (format-doctype this)
                      Element (let [tag (lower-case-keyword (aget this "tagName"))]
                                        (into [] (concat [tag
                                                          (into {} (map as-hiccup (aget this "attributes")))]
                                                         (if (unescapable-content tag)
                                                           (map #(aget % "wholeText") (aget this "childNodes"))
                                                           (map as-hiccup (aget this "childNodes"))))))
                      Text (gstring/htmlEscape (aget this "wholeText")))))

(defn parse-fragment
  [html-str]
  (let [t (js/document.createElement "template")]
    (-> t
        (.-innerHTML)
        (set! html-str))
    (.cloneNode (.-content t) true)))

(comment
  (def s "<!-- Hekkoi --><span>hello</span>")
  (def s "<img src=x onerror=alert(1)>")

  (parse-fragment s)
  (.parseFromString (js/DOMParser.) s "text/html")

  (aget (parse-fragment s) "nodeType")

  (->> (parse-fragment s)
       (as-hiccup))

  (-> (.parseFromString (js/DOMParser.) s "text/html")
      (as-hiccup))
  )