(ns datafrisk.demo
  (:require [reagent.core :as r]
            [datafrisk.core :as d]))

(enable-console-print!)

(defn Animals [data]
  [:div "Awesome animals"
   [:ul
    (map-indexed (fn [i {:keys [animal age]}] ^{:key i} [:li (str animal ", " age " years old")]) (:animals data))]])

;; Needed in order to keep track of the state in the data frisk shell
(def some-atom (r/atom {:data-frisk {:visible? true}}))

(defn mount-root []
  (let [data {:animals '({:animal "Monkey", :age 2}
                         {:animal "Giraffe", :age 4}
                         {:animal "Zebra" :age 3})
              :some-string "a"
              :vector-with-map [1 2 3 3 {:a "a" :b "b"}]
              :a-set #{1 2 3}
              :a-map {:x "x" :y "y" :z [1 2 3 4]}
              :a-seq (seq [1 2])
              :an-object (clj->js {:a "a"})
              :this-is-a-very-long-keyword :g}]

    (r/render
      [:div
       [Animals data]
       [d/DataFriskShell
        #_some-atom
        ;; List of arguments you want to visualize
        data]]
      (js/document.getElementById "app"))))

(defn ^:export main []
  (mount-root))

(defn on-js-reload []
  (mount-root))