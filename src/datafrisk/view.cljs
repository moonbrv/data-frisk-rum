(ns datafrisk.view
  (:require [cljs.pprint :refer [pprint]]
            [rum.core :as rum]
            [datafrisk.util :as u]))

(declare DataFrisk)

(def styles
  {:shell                {:backgroundColor "#FAFAFA"
                          :fontFamily      "Consolas,Monaco,Courier New,monospace"
                          :fontSize        "12px"
                          :z-index         9999}
   :strings              {:color "#4Ebb4E" :white-space "nowrap"}
   :keywords             {:color "purple" :white-space "nowrap"}
   :numbers              {:color "blue"}
   :nil                  {:color "red"}
   :shell-visible-button {:backgroundColor "#4EE24E"}
   :arrow-button {:border          0
                  :textAlign       "center"
                  :backgroundColor "transparent"
                  :width           "10px"
                  :min-width       "10px"
                  :height          "10px"
                  :min-heigth      "10px"
                  :box-sizing      "content-box"
                  :padding         "0"
                  :cursor          "pointer"}})

(rum/defc ErrorIcon < rum/static []
  [:svg {:viewBox "0 0 30 42" :width "100%" :height "100%"}
   [:path {:fill         "darkorange"
           :stroke       "red"
           :stroke-width "2"
           :d            "M15 3
           Q16.5 6.8 25 18
           A12.8 12.8 0 1 1 5 18
           Q13.5 6.8 15 3z"}]
   [:circle {:cx 15 :cy 32 :r 7 :fill "yellow"}]])

(rum/defc ErrorText < rum/static [text]
  [:div {:style {:fontSize    "0.7em"
                 :display     "flex"
                 :align-items "center"
                 :color       "red"}} text])

(rum/defc ExpandButton < rum/static [{:keys [expanded? path emit-fn]}]
  [:button {:style    (:arrow-button styles)
            :on-click #(emit-fn (if expanded? :contract :expand) path)}
   [:svg {:viewBox "0 0 100 100"
          :width   "100%" :height "100%"
          :style   {:transition "all 0.2s ease"
                    :transform  (when expanded? "rotate(90deg)")}}
    [:polygon {:points "0,0 0,100 100,50" :stroke "black"}]]])

(def button-style {:padding          "1px 3px"
                   :cursor           "pointer"
                   :fontSize         "12px"
                   :background-color "white"})

(def input-style {:font    "inherit"
                  :height  "inherit"
                  :padding "0 7px"})

(rum/defc ExpandAllButton < rum/static [emit-fn data]
  [:button {:on-click #(emit-fn :expand-all data)
            :style    (merge button-style
                             {:borderTopLeftRadius    "2px"
                              :borderBottomLeftRadius "2px"
                              :border                 "1px solid darkgray"})}
   "Expand"])

(rum/defc CollapseAllButton < rum/static [emit-fn]
  [:button {:on-click #(emit-fn :collapse-all)
            :style
            (merge button-style
                   {:borderTop    "1px solid darkgray"
                    :borderBottom "1px solid darkgray"
                    :borderRight  "1px solid darkgray"
                    :borderLeft   "0"})}
   "Collapse"])

(rum/defc CopyButton < rum/static [emit-fn data]
  [:button {:on-click #(emit-fn :copy data)
            :style    (merge button-style
                             {:borderTopRightRadius    "2px"
                              :borderBottomRightRadius "2px"
                              :borderTop               "1px solid darkgray"
                              :borderBottom            "1px solid darkgray"
                              :borderRight             "1px solid darkgray"
                              :borderLeft              "0"})}
   "Copy"])

(rum/defc NilText []
  [:span {:style (:nil styles)} (pr-str nil)])

(rum/defc StringText < rum/static [data]
  [:span {:style (:strings styles)} (pr-str data)])

(rum/defc KeywordText < rum/static [data]
  [:span {:style (:keywords styles)} (str data)])

(rum/defc NumberText < rum/static [data]
  [:span {:style (:numbers styles)} data])

(rum/defc KeySet < rum/static [keyset]
  [:span
   (->> keyset
        (sort-by str)
        (map-indexed
         (fn [i data] [:span {:key (str i)}
                       (cond (nil? data) (NilText)
                             (string? data) (StringText data)
                             (keyword? data) (KeywordText data)
                             (number? data) (NumberText data)
                             :else (str data))]))
        (interpose " "))])

(rum/defc Node [{:keys [data path emit-fn swappable metadata-paths]}]
  [:div {:style {:display "flex"}}
   (cond
     (nil? data) (NilText)
     (string? data) (if swappable
                      [:input {:type          "text"
                               :style         input-style
                               :default-value (str data)
                               :on-change     (fn string-changed [e]
                                                (emit-fn :changed path (.. e -target -value)))}]
                      (StringText data))

     (keyword? data) (if swappable
                       [:input {:type          "text"
                                :style         input-style
                                :default-value (name data)
                                :on-change     (fn keyword-changed [e]
                                                 (emit-fn :changed path (keyword (.. e -target -value))))}]
                       (KeywordText data))

     (object? data) (str data " " (.stringify js/JSON data))

     (number? data) (if swappable
                      [:input {:type          "number"
                               :style         input-style
                               :default-value data
                               :on-change
                               (fn number-changed [e]
                                 (emit-fn :changed path (js/Number (.. e -target -value))))}]
                      (NumberText data))
     :else (str data))
   (when-let [errors (:error (get metadata-paths path))]
     (ErrorText (str "\u00A0 " errors)))])

(defn expandable? [v]
  (or (map? v) (seq? v) (coll? v)))

(rum/defc CollectionSummary < rum/static [{:keys [data]} expanded?]
  (cond (map? data) [:div {:style {:flex "0 1 auto"}}
                     [:span "{"]
                     (if expanded?
                       (str (count (keys data)) " keys")
                       (KeySet (keys data)))
                     [:span "}"]]
        (set? data) [:div {:style {:flex "0 1 auto"}} [:span "#{"]
                     (str (count data) " items")
                     [:span "}"]]
        (or (seq? data)
            (vector? data)) [:div {:style {:flex 1}}
                             [:span (if (vector? data) "[" "(")]
                             (str (count data) " items")
                             [:span (if (vector? data) "]" ")")]]))

(rum/defc KeyValNode [{[k v] :data :keys [path metadata-paths emit-fn swappable]}]
  (let [path-to-here (conj path k)
        expandable-node? (and (expandable? v)
                              (not (empty? v)))
        metadata (get metadata-paths path-to-here)
        expanded? (:expanded? metadata)]
    [:div {:style {:display   "flex"
                   :flex-flow "column"}}
     [:div {:style {:display "flex"}}
      [:div {:style {:flex "0 0 10px"}}
       (when expandable-node?
         (ExpandButton {:expanded? expanded?
                        :path      path-to-here
                        :emit-fn   emit-fn}))]
      [:div {:style {:flex "0 1 auto"}}
       [:div {:style {:display   "flex"
                      :flex-flow "row"}}
        [:div {:style {:flex "0 1 auto"}}
         (Node {:data k})]
        [:div {:style {:flex "0 1 auto" :paddingLeft "4px"}}
         (if (expandable? v)
           (CollectionSummary {:data v} expanded?)
           (Node {:data           v
                  :swappable      swappable
                  :path           path-to-here
                  :metadata-paths metadata-paths
                  :emit-fn        emit-fn}))]]]]
     (when expanded?
       [:div {:style {:flex "1"}}
        (DataFrisk {:hide-header?   true
                    :data           v
                    :swappable      swappable
                    :path           path-to-here
                    :metadata-paths metadata-paths
                    :emit-fn        emit-fn})])]))

(rum/defc ListVecNode [{:keys [data path metadata-paths emit-fn swappable hide-header?]}]
  (let [metadata (get metadata-paths path)
        expanded? (:expanded? metadata)]
    [:div {:style {:display   "flex"
                   :flex-flow "column"}}
     (when-not hide-header?
       [:div {:style {:display "flex"}}
        (when (:error metadata)
          [:div {:style {:margin-left "-1em"
                         :width       "1em"
                         :height      "1.2em"}}
           (ErrorIcon)])
        (ExpandButton {:expanded? expanded?
                       :path      path
                       :emit-fn   emit-fn})
        [:div {:style {:flex "0 1 auto"}}
         [:span (if (vector? data) "[" "(")]
         (str (count data) " items")
         [:span (if (vector? data) "]" ")")]]])
     (when expanded?
       [:div {:style {:flex "0 1 auto" :padding "0 0 0 20px"}}
        (when (:error metadata)
          [:div {:style {:paddingBottom "4px"}}
           (ErrorText (:error metadata))])
        (map-indexed (fn [i x] (rum/with-key (DataFrisk {:data           x
                                                         :swappable      swappable
                                                         :path           (conj path i)
                                                         :metadata-paths metadata-paths
                                                         :emit-fn        emit-fn}) (str i))) data)])]))

(rum/defc SetNode [{:keys [data path metadata-paths emit-fn swappable hide-header?]}]
  (let [metadata (get metadata-paths path)
        expanded? (:expanded? metadata)]
    [:div {:style {:display   "flex"
                   :flex-flow "column"}}
     (when-not hide-header?
       [:div {:style {:display "flex"}}
        (when (:error metadata)
          [:div {:style {:margin-left "-1em"
                         :width       "1em"
                         :height      "1.2em"}}
           (ErrorIcon)])
        (ExpandButton {:expanded? expanded?
                       :path      path
                       :emit-fn   emit-fn})
        [:div {:style {:flex "0 1 auto"}}
         [:span "#{"]
         (str (count data) " items")
         [:span "}"]]])
     (when expanded?
       [:div {:style {:flex "0 1 auto" :paddingLeft "20px"}}
        (when (:error metadata)
          [:div {:style {:paddingBottom "4px"}}
           (ErrorText (:error metadata))])
        (map-indexed (fn [i x] (rum/with-key (DataFrisk {:data           x
                                                         :swappable      swappable
                                                         :path           (conj path i)
                                                         :metadata-paths metadata-paths
                                                         :emit-fn        emit-fn}) (str i))) data)])]))

(rum/defc MapNode < rum/static [{:keys [data path metadata-paths emit-fn hide-header?] :as all}]
  (let [metadata (get metadata-paths path)
        expanded? (:expanded? metadata)]
    [:div {:style {:display   "flex"
                   :flex-flow "column"}}
     (when-not hide-header?
       [:div {:style {:display "flex"}}
        (when (:error metadata)
          [:div {:style {:margin-left "-1em"
                         :width       "1em"
                         :height      "1.2em"}}
           (ErrorIcon)])
        (ExpandButton {:expanded? expanded?
                       :path      path
                       :emit-fn   emit-fn})
        [:div {:style {:flex "0 1 auto"}}
         [:span (str "{")]
         (KeySet (keys data))
         [:span "}"]]])
     (when expanded?
       [:div {:style {:flex "0 1 auto" :paddingLeft "20px"}}
        (when (:error metadata)
          [:div {:style {:paddingBottom "4px"}}
           (ErrorText (:error metadata))])
        (->> data
             (sort-by (fn [[k _]] (str k)))
             (map-indexed (fn [i x] (rum/with-key (KeyValNode (assoc all :data x)) (str i)))))])]))

(rum/defc DataFrisk < rum/reactive [{:keys [data] :as all}]
  (cond (map? data) (MapNode all)
        (set? data) (SetNode all)
        (or (seq? data) (vector? data)) (ListVecNode all)
        (satisfies? IDeref data) (DataFrisk (assoc all :data (rum/react data)))
        :else [:div {:style {:paddingLeft "20px"}} (Node all)]))

(defn conj-to-set [coll x]
  (conj (or coll #{}) x))

(defn expand-all-paths [root-value current-expanded-paths]
  (loop [remaining [{:path [] :node root-value}]
         expanded-paths current-expanded-paths]
    (if (seq remaining)
      (let [[current & rest] remaining
            current-node (if (satisfies? IDeref (:node current)) @(:node current) (:node current))]
        (cond (map? current-node)
              (recur
               (concat rest (map (fn [[k v]] {:path (conj (:path current) k)
                                              :node v})
                                 current-node))
               (assoc-in expanded-paths [(:path current) :expanded?] true))

              (or (seq? current-node)
                  (vector? current-node)
                  (set? current-node))
              (recur
               (concat rest (map-indexed (fn [i node] {:path (conj (:path current) i)
                                                       :node node})
                                         current-node))
               (assoc-in expanded-paths [(:path current) :expanded?] true))

              :else
              (recur
               rest
               (if (coll? current-node)
                 (assoc-in expanded-paths [(:path current) :expanded?] true)
                 expanded-paths))))
      expanded-paths)))

(defn copy-to-clipboard [data]
  (let [pretty (with-out-str (pprint data))
        textArea (.createElement js/document "textarea")]
    (doto textArea
      ;; Put in top left corner of screen
      (aset "style" "position" "fixed")
      (aset "style" "top" 0)
      (aset "style" "left" 0)
      ;; Make it small
      (aset "style" "width" "2em")
      (aset "style" "height" "2em")
      (aset "style" "padding" 0)
      (aset "style" "border" "none")
      (aset "style" "outline" "none")
      (aset "style" "boxShadow" "none")
      ;; Avoid flash of white box
      (aset "style" "background" "transparent")
      (aset "value" pretty))

    (.appendChild (.-body js/document) textArea)
    (.select textArea)

    (.execCommand js/document "copy")
    (.removeChild (.-body js/document) textArea)))

(defn collapse-all [metadata-paths]
  (u/map-vals #(assoc % :expanded? false) metadata-paths))

(defn emit-fn-factory [state-atom id swappable]
  (fn [event & args]
    (case event
      :expand (swap! state-atom assoc-in [:data-frisk id :metadata-paths (first args) :expanded?] true)
      :expand-all (swap! state-atom update-in [:data-frisk id :metadata-paths] (partial expand-all-paths (first args)))
      :contract (swap! state-atom assoc-in [:data-frisk id :metadata-paths (first args) :expanded?] false)
      :collapse-all (swap! state-atom update-in [:data-frisk id :metadata-paths] collapse-all)
      :copy (copy-to-clipboard (first args))
      :changed (let [[path value] args]
                 (if (seq path)
                   (swap! swappable assoc-in path value)
                   (reset! swappable value))))))

(rum/defc Root < rum/reactive [data id state-atom]
  (let [data-frisk (:data-frisk (rum/react state-atom))
        swappable (when (satisfies? IAtom data)
                    data)
        emit-fn (emit-fn-factory state-atom id swappable)
        metadata-paths (get-in data-frisk [id :metadata-paths])]
    [:div
     [:div {:style {:padding "4px 2px"}}
      (ExpandAllButton emit-fn data)
      (CollapseAllButton emit-fn)
      (CopyButton emit-fn data)]
     [:div {:style {:flex "0 1 auto"}}
      (DataFrisk {:data           data
                  :swappable      swappable
                  :path           []
                  :metadata-paths metadata-paths
                  :emit-fn        emit-fn})]]))

(rum/defc VisibilityButton < rum/static
  [visible? update-fn]
  [:button {:style    (:arrow-button styles)
            :on-click update-fn}
   [:svg {:viewBox "0 0 100 100"
          :width   "100%" :height "100%"
          :style   {:transition "all 0.2s ease"
                    :transform  (when visible? "rotate(90deg)")}}
    [:polygon {:points "0,0 0,100 100,50" :stroke "black"}]]])

(rum/defcs DataFriskView < (rum/local {} ::state-atom)
  {:will-mount (fn [state]
                 (let [data (-> state :rum/args)
                       expand-by-default (reduce #(assoc-in %1 [:data-frisk %2 :metadata-paths [] :expanded?] true) {} (range (count data)))]
                   (reset! (::state-atom state) expand-by-default))
                 state)}
  [state & data]
  (let [state-atom (::state-atom state)
        visible? (get-in @state-atom [:data-frisk :visible?])]
    [:div {:style (merge {:flex-flow  "row nowrap"
                          :transition "all 0.3s ease-out"
                          :z-index    "5"}
                         (when-not visible?
                           {:overflow-x "hide"
                            :overflow-y "hide"
                            :max-height "30px"
                            :max-width  "100px"})
                         (:shell styles))}
     (VisibilityButton visible? (fn [_] (swap! state-atom assoc-in [:data-frisk :visible?] (not visible?))))
     [:span "Data frisk"]
     (when visible?
       [:div {:style {:padding    "10px"
                      ;; TODO Make the max height and width adjustable
                      ;:max-height "400px"
                      ;:max-width "800px"
                      :resize     "both"
                      :box-sizing "border-box"
                      :overflow-x "auto"
                      :overflow-y "auto"}}
        (map-indexed (fn [id x]
                       (rum/with-key (Root x id state-atom) (str id))) data)])]))
