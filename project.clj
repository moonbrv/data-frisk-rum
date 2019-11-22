(defproject data-frisk-rum "0.0.5"
  :description "Frisking EDN since 2016!"
  :url "https://github.com/moonbrv/data-frisk-rum"
  :license {:name "MIT"
            :url  "https://opensource.org/licenses/MIT"}
  :min-lein-version "2.7.1"
  :dependencies [[rum "0.11.3"]]
  :plugins [[lein-figwheel "0.5.14"]
            [lein-doo "0.1.11"]
            [lein-ancient "0.6.15"]
            [lein-cljsbuild "1.1.7" :exclusions [[org.clojure/clojure]]]]
  :source-paths ["src"]

  :figwheel {:http-server-root "public"
             :server-port      3999}

  :aliases {"testing"   ["do" ["clean"] ["doo" "chrome-headless" "test" "once"]]
            "clj-kondo" ["run" "-m" "clj-kondo.main" "--lint" "src" "--config" ".clj-kondo/config.edn" "--cache"]}

  :profiles {:dev {:dependencies   [[org.clojure/clojure "1.10.1"]
                                    [org.clojure/clojurescript "1.10.520"]
                                    [clj-kondo "2019.11.07"]
                                    [doo "0.1.11"]
                                    [com.cemerick/piggieback "0.2.2"]
                                    [figwheel-sidecar "0.5.14"]
                                    [devcards "0.2.5" :exclusions [[cljsjs/react]]]]
                   :source-paths   ["src" "devcards"]
                   :resource-paths ["devresources"]
                   :cljsbuild      {:builds [{:id           "dev"
                                              :source-paths ["src" "dev"]
                                              :figwheel     {:on-jsload "datafrisk.demo/on-js-reload"}
                                              :compiler     {:main       "datafrisk.demo"
                                                             :asset-path "js/out"
                                                             :output-to  "resources/public/js/main.js"
                                                             :output-dir "resources/public/js/out"}}
                                             {:id           "cards"
                                              :source-paths ["src" "devcards"]
                                              :figwheel     {:devcards true}
                                              :compiler     {:main       "datafrisk.cards"
                                                             :asset-path "js/out-cards"
                                                             :output-to  "resources/public/js/cards.js"
                                                             :output-dir "resources/public/js/out-cards"}}
                                             {:id           "test"
                                              :source-paths ["src" "test"]
                                              :compiler     {:output-to     "resources/public/js/compiled/test.js"
                                                             :main          datafrisk.test-runner
                                                             :optimizations :none}}
                                             {:id           "demo-site"
                                              :source-paths ["src" "dev"]
                                              :compiler     {:main          "datafrisk.demo"
                                                             :optimizations :advanced
                                                             :output-to     "demo-site/main.js"
                                                             :output-dir    "demo-site/out"}}]}}}
  :clean-targets ^{:protect false} ["resources/public/js" "target"])
