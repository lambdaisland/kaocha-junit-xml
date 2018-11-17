(ns kaocha.plugin.junit-xml-test
  (:require [kaocha.plugin.junit-xml :as junit-xml]
            [kaocha.repl :as repl]
            [kaocha.api :as api]
            [clojure.test :refer :all]
            [matcher-combinators.test]
            [clojure.string :as str]
            [kaocha.plugin :as plugin])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(java.util.TimeZone/setDefault (java.util.TimeZone/getTimeZone "UTC"))

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
                  xml->hiccup)))

  (testing "it renders start-time when present"
    (is (= [:testuites {:skipped 0, :errors 0, :failures 0, :tests 1}
            [:testsuite {:name "my-test-type" :id 0 :hostname "localhost"
                         :skipped 0 :errors 0 :failures 0 :tests 0
                         :timestamp "2007-12-03T10:15:30" :time "0.000012"}]]
           (-> {:kaocha.result/tests [{:kaocha.testable/id :my-test-type
                                       :kaocha.testable/type ::foo
                                       :kaocha.result/count 1
                                       :kaocha.result/fail 0
                                       :kaocha.result/pass 1
                                       :kaocha.result/error 0
                                       :kaocha.plugin.profiling/start (java.time.Instant/parse "2007-12-03T10:15:30.00Z")
                                       :kaocha.plugin.profiling/duration 12345}]}
               junit-xml/result->xml
               xml->hiccup)))))

(deftest junit-xml-plugin
  (testing "cli-options"
    (is (= [[nil "--junit-xml-file FILENAME" "Save the test results to a Ant JUnit XML file."]]
           (let [chain (plugin/load-all [:kaocha.plugin/junit-xml])]
             (plugin/run-hook* chain :kaocha.hooks/cli-options [])))))

  (testing "config"
    (is (= {:kaocha/cli-options {:junit-xml-file "my-out-file.xml"}
            :kaocha.plugin.junit-xml/target-file "my-out-file.xml"}
           (let [chain (plugin/load-all [:kaocha.plugin/junit-xml])]
             (plugin/run-hook* chain :kaocha.hooks/config {:kaocha/cli-options {:junit-xml-file "my-out-file.xml"}}))))

    (testing "does nothing if junit-xml-file is not present"
      (is (= {:foo :bar}
             (let [chain (plugin/load-all [:kaocha.plugin/junit-xml])]
               (plugin/run-hook* chain :kaocha.hooks/config {:foo :bar}))))))

  (testing "post-run"
    (let [outfile (Files/createTempFile (namespace `_) ".xml" (make-array FileAttribute 0))
          chain (plugin/load-all [:kaocha.plugin/junit-xml])
          result {:kaocha.plugin.junit-xml/target-file (str outfile)
                  :kaocha.result/tests [{:kaocha.testable/id :my-test-type
                                         :kaocha.testable/type ::foo
                                         :kaocha.result/count 1
                                         :kaocha.result/fail 0
                                         :kaocha.result/pass 1
                                         :kaocha.result/error 0
                                         :kaocha.plugin.profiling/start (java.time.Instant/parse "2007-12-03T10:15:30.00Z")
                                         :kaocha.plugin.profiling/duration 12345}]}]
      (plugin/run-hook* chain :kaocha.hooks/post-run result)
      (is (= [:testuites {:tests "1"
                          :failures "0"
                          :errors "0"
                          :skipped "0"}
              [:testsuite {:errors "0"
                           :tests "0"
                           :name "my-test-type"
                           :time "0.000012"
                           :hostname "localhost"
                           :skipped "0"
                           :id "0"
                           :timestamp "2007-12-03T10:15:30"
                           :failures "0"}]]
             (xml->hiccup (clojure.xml/parse (str outfile))))))))
