(defproject hs-client "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [org.clojure/clojurescript "1.10.773"
                  :exclusions [com.google.javascript/closure-compiler-unshaded
                               org.clojure/google-closure-library
                               org.clojure/google-closure-library-third-party]]
                 [thheller/shadow-cljs "2.11.24"]
                 [reagent "1.0.0"]
                 [re-frame "1.2.0"]
                 [cljs-ajax "0.8.1"]
                 [metosin/reitit "0.5.12"]
                 [day8.re-frame/http-fx "0.2.3"]
                 [camel-snake-kebab "0.4.2"]
                 [aero "1.1.6"] ; Либа для чтения edn-конфигов с тегами.
                 [ring "1.9.2"]
                 [compojure "1.6.2"]]

  :plugins [[lein-shadow "0.3.1"]
            [lein-shell "0.5.0"]]

  :min-lein-version "2.9.0"

  :source-paths ["src/cljc" "src/cljs" "src/clj"]

  :test-paths   ["test/cljs"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target" "test/js"]

  :shadow-cljs {:nrepl {:port 8777}

                :builds {:app {:target :browser
                               :output-dir "resources/public/js/compiled"
                               :asset-path "/js/compiled"
                               :modules {:app {:init-fn hs-client.core/-main
                                               :preloads [devtools.preload]}}

                               :devtools {:http-root "resources/public"
                                          :http-port 8280
                                          :http-handler hs-client.handler/dev-handler}}

                         :browser-test {:target :browser-test
                                        :ns-regexp "-test$"
                                        :runner-ns shadow.test.browser
                                        :test-dir "target/browser-test"
                                        :devtools {:http-root "target/browser-test"
                                                   :http-port 8290}}

                         :karma-test {:target :karma
                                      :ns-regexp "-test$"
                                      :output-to "target/karma-test.js"}}}

  :shell {:commands {"karma" {:windows         ["cmd" "/c" "karma"]
                              :default-command "karma"}
                     "open"  {:windows         ["cmd" "/c" "start"]
                              :macosx          "open"
                              :linux           "xdg-open"}}}

  :aliases {"watch"        ["with-profile" "dev" "do"
                            ["shadow" "watch" "app" "browser-test" "karma-test"]]

            "release"      ["with-profile" "prod" "do"
                            ["shadow" "release" "app"]]

            "build-report" ["with-profile" "prod" "do"
                            ["shadow" "run" "shadow.cljs.build-report" "app" "target/build-report.html"]
                            ["shell" "open" "target/build-report.html"]]

            "ci"           ["with-profile" "prod" "do"
                            ["shadow" "compile" "karma-test"]
                            ["shell" "karma" "start" "--single-run" "--reporters" "junit,dots"]]}

  :profiles {:dev {:dependencies [[binaryage/devtools "1.0.2"]]
                   :source-paths ["dev"]}

             :prod {}

             :uberjar {:source-paths ["env/prod/clj"]
                       :omit-source  true
                       :main         hs-client.server
                       :aot          [hs-client.server]
                       :uberjar-name "hs-client.jar"
                       :prep-tasks   ["compile" ["release"]]}}

  :prep-tasks [])
