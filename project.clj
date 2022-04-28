(defproject tech.droit/kaocha-junit-xml "0.0.78"
  :description "Kaocha plugin to generate a JUnit XML version of the test results."
  :url "https://github.com/droitfintech/kaocha-junit-xml"
  :dependencies
  [[org.clojure/clojure "1.10.0"]
   [lambdaisland/kaocha "1.65.1029"]]
  :profiles
  {:test
   {:dependencies
    [[nubank/matcher-combinators    "0.6.1"]
     [org.clojure/data.xml          "0.0.8"]
     [lambdaisland/kaocha-cloverage "0.0-22"]]}}

  :aliases
  {"kaocha" ["run" "-m" "kaocha.runner"]
   "test" ["with-profiles" "+test" "kaocha"]})
