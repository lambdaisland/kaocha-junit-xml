(ns kaocha.plugin.junit-xml-test
  (:require [kaocha.plugin.junit-xml :as junit-xml]
            [kaocha.repl :as repl]
            [kaocha.api :as api]
            [clojure.test :refer :all]
            [matcher-combinators.test]
            [clojure.string :as str]))

(defn xml->hiccup [xml]
  (if (map? xml)
    (let [{:keys [tag attrs content]} xml]
      `[~tag ~@(when (seq attrs) [attrs]) ~@(map xml->hiccup content)])
    xml))

(deftest xml-output-test
  (is (match? [:testuites {:skipped 1 :errors 2 :failures 1 :tests 4}
               [:testsuite {:name "unit" :id 0 :hostname "localhost" :skipped 1 :errors 2 :failures 1 :tests 4}
                [:testcase {:name "demo.test/basic-test" :classname "demo.test" :assertions 1}]
                [:testcase {:name "demo.test/exception-in-is-test" :classname "demo.test" :assertions 1}
                 [:error {:message "Inside assertion"
                          :type "java.lang.Exception"}
                  #(str/starts-with? % "expected: (throw (Exception.")]]
                [:testcase {:name "demo.test/exception-outside-is-test" :classname "demo.test" :assertions 1}
                 [:error {:message "outside assertion"
                          :type "java.lang.Exception"}
                  #(str/starts-with? % "expected: nil\n  actual: #error")]]
                [:testcase {:name "demo.test/output-test" :classname "demo.test" :assertions 1}
                 [:failure {:message
                            "Expected:\n  {:foo 1}\nActual:\n  {:foo -1 +2}\n"
                            :type '=}]
                 [:system-out "this is on stdout\nthis is on stderr\n"]]
                [:testcase {:name "demo.test/skip-test" :classname "demo.test" :assertions 0}
                 [:skipped]]]]

              (-> {:tests [{:test-paths ["resources/kaocha-demo"]
                            :ns-patterns [".*"]}]
                   :kaocha.plugin.randomize/randomize? false
                   :color? false
                   :reporter identity}
                  repl/config
                  api/run
                  junit-xml/result->xml
                  xml->hiccup))))
