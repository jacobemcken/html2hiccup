(ns app.core
  (:require ["codemirror" :refer [basicSetup]]
            ["@codemirror/state" :refer [EditorState]]
            ["@codemirror/view" :refer [EditorView]]
            ["@codemirror/lang-html" :refer [html] :rename {html lang-html}]
            ["@nextjournal/lang-clojure" :refer [clojure] :rename {clojure lang-clojure}]
            [app.convert :as convert]
            [reagent.core :as reagent]
            [reagent.dom :as dom]))

(def default-html
  "<div>
  <span>Hello World!</span>
  <!-- Can handle comments -->
  <a href=\"https://www.clojure.org\">Clojure webiste</a>
  <svg width=\"64\" height=\"64\" viewBox=\"0 0 64 64\" xmlns=\"http://www.w3.org/2000/svg\" baseProfile=\"full\">
    <rect x=\"16\" y=\"16\" width=\"32\" height=\"32\" fill=\"#ef4444\"/>
  </svg>
</div>")

;; Codemirror used for Clojure (in ClojureScript):
;; https://github.com/nextjournal/clojure-mode/blob/main/demo/src/nextjournal/clojure_mode/demo.cljs
;; https://www.raresportan.com/how-to-make-a-code-editor-with-codemirror6/

(defn create-state
  [opts]
  (->> (clj->js opts)
       (.create EditorState)))

(defn create-view
  "Takes a parent element and an initial state and returns a CodeMirror EditorView."
  [parent-element state]
  (new EditorView
       #js {:state (create-state state)
            :parent parent-element}))

(defn editor [source on-change]
  (reagent/with-let [!view (reagent/atom nil)
                     mount! (fn [el]
                              (when el
                                (reset! !view (create-view el {:doc source
                                                               :extensions [basicSetup (lang-html)
                                                                            (.. EditorView -updateListener (of (fn [^js e]
                                                                                                                 ;; startState is equal to state is a hack to identify when CodeMirrow has been initialized
                                                                                                                 ;; it is also true at other times but it does greatly reduce the amount of "on change" events
                                                                                                                 (when (or (.-docChanged e) (= (.-startState e) (.-state e)))
                                                                                                                   (on-change (.. e -state -doc toString))))))]}))))]
    [:div
     [:label {:for "html" :class "block text-sm font-medium text-gray-700"}
      "HTML"]
     [:div {:class "rounded-md mb-0 text-sm monospace overflow-auto relative border shadow-lg bg-white"
            :ref mount! ; See note [1] for info on :ref
            :style {:max-height 750}}]]
    #_(finally
        (.destroy @!view))))

(def hiccup-view-props
  {:extensions [basicSetup
                (lang-clojure)
                (.. EditorState -readOnly (of true))]})

(defn hiccup-viewer
  [!view]
  (reagent/with-let [mount! (fn [el]
                              (when el
                                (reset! !view (create-view el hiccup-view-props))))]
    [:div
     [:label {:for "hiccup" :class "block text-sm font-medium text-gray-700"}
      "Hiccup"]
     [:div {:class "rounded-md mb-0 text-sm monospace overflow-auto relative border shadow-lg bg-white"
            :ref mount! ; See note [1] for info on :ref
            :style {:max-height 750}}]]))

(defn columns
  []
  (reagent/with-let [!hiccup-view (reagent/atom nil)]
    (let [input (reagent/atom default-html)]
      [:div {:class "grid grid-cols-2 gap-4"}
       [editor default-html #(.. @!hiccup-view (setState (create-state (assoc hiccup-view-props :doc (convert/html->hiccup %)))))]
       [hiccup-viewer !hiccup-view]])))

(defn footer
  []
  [:footer
   {:aria-labelledby "footer-heading"}
   [:h2 {:id "footer-heading", :class "sr-only"} "Footer"]
   [:div
    {:class "mx-auto max-w-7xl py-6 px-4 sm:px-4 lg:py-8 lg:px-8"}
    [:div
     {:class "mt-8 border-t border-gray-300 pt-4"}
     [:p
      {:class "text-base text-gray-400 xl:text-center"}
      [:a
       {:href "https://html2hiccup.dev" :class "text-base text-gray-500 hover:text-gray-900"}
       "html2hiccup.dev"]
      " for your convenience"]]]])

(defn page
  []
  [:<>
   [:div
    {:class "bg-gray-800 pb-32"}
    [:header
     {:class "py-10"}
     [:div
      {:class "mx-auto max-w-7xl px-4 sm:px-6 lg:px-8"}
      [:img {:src "img/heart.svg" :height "100px" :width "100px" :class "float-right"}]
      [:h1
       {:class "text-3xl font-bold tracking-tight text-white"}
       "HTML to Hiccup converter"]
      [:h2 {:class "text-normal text-red-300"} "Clojure and ClojureScript fiendly"]]]]
   [:main
    {:class "-mt-32 mb-auto"}
    [:div
     {:class "mx-auto px-4 pb-12 sm:px-6 lg:px-8"}
     [:div
      {:class "rounded-lg bg-white px-5 py-6 shadow sm:px-6"}
      [columns]]]]
   [footer]])

(defn ^:dev/after-load start
  []
  (dom/render [page] (.getElementById js/document "app")))

(defn ^:export init
  []
  (start))

;; [1]: https://cljdoc.org/d/reagent/reagent/1.1.1/doc/frequently-asked-questions/how-do-i-use-react-s-refs-
;;      https://stackoverflow.com/questions/39173424/which-changes-to-clojurescript-atoms-cause-reagent-components-to-re-render
;;      https://reactjs.org/docs/refs-and-the-dom.html


;; Maybe use https://github.com/kkinnear/zprint later
