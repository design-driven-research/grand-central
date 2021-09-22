(defproject grand-central "0.1.0-SNAPSHOT"

  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :repositories [["cognitect-dev-tools" {:url      "https://dev-tools.cognitect.com/maven/releases/"
                                         :username :env
                                         :password :env}]]

  :dependencies [[ch.qos.logback/logback-classic "1.2.5"]
                 [clojure.java-time "0.3.3"]
                 [gorillalabs/neo4j-clj "4.1.2"]
                 [com.google.guava/guava "25.1-jre"]
                 [nano-id "1.0.0"]
                 [cprop "0.1.19"]
                 [expound "0.8.9"]
                 [funcool/cuerdas "2021.05.29-0"]
                 [funcool/struct "1.4.0"]
                 [json-html "0.4.7"]
                 [luminus-aleph "0.1.6"]
                 [luminus-transit "0.1.2"]
                 [luminus/ring-ttl-session "0.3.3"]
                 [markdown-clj "1.10.6"]
                 [metosin/muuntaja "0.6.8"]
                 [metosin/reitit "0.5.15"]
                 [metosin/ring-http-response "0.9.3"]
                 [mount "0.1.16"]
                 [nrepl "0.8.3"]
                 [org.clojure/clojure "1.10.3"]
                 [org.clojure/tools.cli "1.0.206"]
                 [org.clojure/tools.logging "1.1.0"]
                 [org.webjars.npm/bulma "0.9.2"]
                 [org.webjars.npm/material-icons "1.0.0"]
                 [org.webjars/webjars-locator "0.41"]
                 [ring-webjars "0.2.0"]
                 [ring/ring-core "1.9.4"]
                 [ring/ring-defaults "0.3.3"]
                 [selmer "1.12.44"]
                 [com.taoensso/sente "1.16.2"]
                 [metosin/reitit-sieppari "0.5.15"]
                 [postmortem "0.5.0"]]

  :min-lein-version "2.0.0"

  :source-paths ["src/clj"]
  :test-paths ["test/clj"]
  :resource-paths ["resources"]
  :target-path "target/%s/"
  :main ^:skip-aot rdd.grand-central.core

  :plugins []

  :profiles
  {:reveal {:dependencies [[vlaaad/reveal "1.3.194"]]
            :repl-options {:nrepl-middleware [vlaaad.reveal.nrepl/middleware]}}
   :uberjar {:omit-source true
             :aot :all
             :uberjar-name "grand-central.jar"
             :source-paths ["env/prod/clj"]
             :resource-paths ["env/prod/resources"]}

   :dev           [:project/dev :profiles/dev]
   :test          [:project/dev :project/test :profiles/test]

   :project/dev  {:jvm-opts ["-Dconf=dev-config.edn"]
                  :dependencies [[org.clojure/tools.namespace "1.1.0"]
                                 [pjstadig/humane-test-output "0.11.0"]
                                 [prone "2021-04-23"]
                                 [ring/ring-devel "1.9.4"]
                                 [ring/ring-mock "0.4.0"]]
                  :plugins      [[com.jakemccrary/lein-test-refresh "0.24.1"]
                                 [jonase/eastwood "0.3.5"]
                                 [cider/cider-nrepl "0.26.0"]]

                  :source-paths ["env/dev/clj"]
                  :resource-paths ["env/dev/resources"]
                  :repl-options {:init-ns user
                                 :timeout 120000}
                  :injections [(require 'pjstadig.humane-test-output)
                               (pjstadig.humane-test-output/activate!)]}
   :project/test {:jvm-opts ["-Dconf=test-config.edn"]
                  :resource-paths ["env/test/resources"]}
   :profiles/dev {}
   :profiles/test {}})

