(ns kaocha.plugin.junit-xml-test
  (:require [kaocha.plugin.junit-xml :as junit-xml]
            [kaocha.repl :as repl]
            [kaocha.api :as api]
            [clojure.test :refer :all]
            [matcher-combinators.test]
            [clojure.string :as str]
            [kaocha.plugin :as plugin]
            [kaocha.plugin.junit-xml.xml :as xml]
            [clojure.java.io :as io]
            [clojure.xml])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]
           [javax.xml.parsers SAXParser SAXParserFactory]))

(java.util.TimeZone/setDefault (java.util.TimeZone/getTimeZone "UTC"))

(defn xml->hiccup [xml]
  (if (map? xml)
    (let [{:keys [tag attrs content]} xml]
      `[~tag ~@(when (seq attrs) [attrs]) ~@(map xml->hiccup content)])
    xml))

(defn abs-path [test-path test-file]
  (.. (io/file test-path test-file)
      getAbsolutePath
      toString))

(deftest xml-output-test
  (is (match?
       [:testsuites
        [:testsuite {:id 0
                     :tests 5
                     :failures 1
                     :errors 2
                     :package ""
                     :name "unit"
                     :hostname "localhost"
                     :skipped 1
                     :timestamp string?
                     :time "0.000000"}
         [:properties]
         [:testcase {:name "demo.test/basic-test" :classname "demo.test" :time "0.000000"}]
         [:testcase {:name "demo.test/exception-in-is-test"
                     :classname "demo.test"
                     :time "0.000000"}
          [:error {:message "Inside assertion" :type "java.lang.Exception"}
           #(str/starts-with? % (str "ERROR in demo.test/exception-in-is-test (test.clj:15)\n"
                                     "Exception: java.lang.Exception: Inside assertion\n"
                                     " at demo.test"))]]
         [:testcase {:name "demo.test/exception-outside-is-test"
                     :classname "demo.test"
                     :time "0.000000"}
          [:error {:message "Uncaught exception, not in assertion."
                   :type "java.lang.Exception"}
           #(str/starts-with? % (str "ERROR in demo.test/exception-outside-is-test (test.clj:19)\n"
                                     "Uncaught exception, not in assertion.\n"
                                     "Exception: java.lang.Exception: outside assertion\n"
                                     " at demo.test"))]]
         [:testcase {:name "demo.test/output-test"
                     :classname "demo.test"
                     :time "0.000000"}
          [:failure {:message "[:fail] expected: (= {:foo 1} {:foo 2}). actual: (not (= {:foo 1} {:foo 2}))"
                     :type "assertion failure: ="}
           (str "FAIL in demo.test/output-test (test.clj:13)\n"
                "oops\n"
                "Expected:\n"
                "  {:foo 1}\n"
                "Actual:\n"
                "  {:foo -1 +2}\n"
                "╭───── Test output ───────────────────────────────────────────────────────\n"
                "│ this is on stdout\n"
                "│ this is on stderr\n"
                "╰─────────────────────────────────────────────────────────────────────────")]]
         [:testcase {:name "demo.test/pending-test"
                     :classname "demo.test"
                     :time "0.000000"}
          [:skipped]]
         [:testcase {:name "demo.test/skip-test"
                     :classname "demo.test"
                     :time "0.000000"}]
         [:system-out "this is on stdout\nthis is on stderr\n"]
         [:system-err]]]

       (-> {:tests [{:test-paths ["fixtures/kaocha-demo"]
                     :ns-patterns [".*"]}]
            :kaocha.plugin.randomize/randomize? false
            :color? false
            :reporter identity}
           repl/config
           api/run
           junit-xml/result->xml
           xml->hiccup)))

  (testing "it outputs location metadata when configured"
    (is (match?
         [:testsuites
          [:testsuite {:id 0
                       :tests 5
                       :failures 1
                       :errors 2
                       :package ""
                       :name "unit"
                       :hostname "localhost"
                       :skipped 1
                       :timestamp string?
                       :time "0.000000"}
           [:properties]
           [:testcase {:name "demo.test/basic-test" :classname "demo.test" :time "0.000000"}]
           [:testcase {:name "demo.test/exception-in-is-test"
                       :classname "demo.test"
                       :time "0.000000"
                       :column 1
                       :file (abs-path "fixtures/kaocha-demo" "demo/test.clj")
                       :line 15}
            [:error {:message "Inside assertion" :type "java.lang.Exception"}
             #(str/starts-with? % (str "ERROR in demo.test/exception-in-is-test (test.clj:15)\n"
                                       "it outputs location metadata when configured\n"
                                       "Exception: java.lang.Exception: Inside assertion\n"
                                       " at demo.test"))]]
           [:testcase {:name "demo.test/exception-outside-is-test"
                       :classname "demo.test"
                       :time "0.000000"
                       :column 1
                       :file (abs-path "fixtures/kaocha-demo" "demo/test.clj")
                       :line 19}
            [:error {:message "Uncaught exception, not in assertion."
                     :type "java.lang.Exception"}
             #(str/starts-with? % (str "ERROR in demo.test/exception-outside-is-test (test.clj:19)\n"
                                       "it outputs location metadata when configured\n"
                                       "Uncaught exception, not in assertion.\n"
                                       "Exception: java.lang.Exception: outside assertion\n"
                                       " at demo.test"))]]
           [:testcase {:name "demo.test/output-test"
                       :classname "demo.test"
                       :time "0.000000"
                       :column 1
                       :line 7
                       :file (abs-path "fixtures/kaocha-demo" "demo/test.clj")}
            [:failure {:message "[:fail] expected: (= {:foo 1} {:foo 2}). actual: (not (= {:foo 1} {:foo 2}))"
                       :type "assertion failure: ="}
             (str "FAIL in demo.test/output-test (test.clj:13)\n"
                  "it outputs location metadata when configured\n"
                  "oops\n"
                  "Expected:\n"
                  "  {:foo 1}\n"
                  "Actual:\n"
                  "  {:foo -1 +2}\n"
                  "╭───── Test output ───────────────────────────────────────────────────────\n"
                  "│ this is on stdout\n"
                  "│ this is on stderr\n"
                  "╰─────────────────────────────────────────────────────────────────────────")]]
           [:testcase {:name "demo.test/pending-test"
                       :classname "demo.test"
                       :time "0.000000"}
            [:skipped]]
           [:testcase {:name "demo.test/skip-test"
                       :classname "demo.test"
                       :time "0.000000"}]
           [:system-out "this is on stdout\nthis is on stderr\n"]
           [:system-err]]]

         (-> {:tests [{:test-paths ["fixtures/kaocha-demo"]
                       :ns-patterns [".*"]}]
              :kaocha.plugin.randomize/randomize? false
              :kaocha.plugin.junit-xml/add-location-metadata? true
              :color? false
              :reporter identity}
             repl/config
             api/run
             junit-xml/result->xml
             xml->hiccup))))
    (testing "it outputs location metadata when configured"
      (is (match?
           [:testsuites
            [:testsuite {:id 0
                         :tests 5
                         :failures 1
                         :errors 2
                         :package ""
                         :name "unit"
                         :hostname "localhost"
                         :skipped 1
                         :timestamp string?
                         :time "0.000000"}
             [:properties]
             [:testcase {:name "demo.test/basic-test" :classname "demo.test" :time "0.000000"}]
             [:testcase {:name "demo.test/exception-in-is-test"
                         :classname "demo.test"
                         :time "0.000000"
                         :column 1
                         :file "fixtures/kaocha-demo/demo/test.clj"
                         :line 15}
              [:error {:message "Inside assertion" :type "java.lang.Exception"}
               #(str/starts-with? % (str "ERROR in demo.test/exception-in-is-test (test.clj:15)\n"
                                         "it outputs location metadata when configured\n"
                                         "Exception: java.lang.Exception: Inside assertion\n"
                                         " at demo.test"))]]
             [:testcase {:name "demo.test/exception-outside-is-test"
                         :classname "demo.test"
                         :time "0.000000"
                         :column 1
                         :file "fixtures/kaocha-demo/demo/test.clj"
                         :line 19}
              [:error {:message "Uncaught exception, not in assertion."
                       :type "java.lang.Exception"}
               #(str/starts-with? % (str "ERROR in demo.test/exception-outside-is-test (test.clj:19)\n"
                                         "it outputs location metadata when configured\n"
                                         "Uncaught exception, not in assertion.\n"
                                         "Exception: java.lang.Exception: outside assertion\n"
                                         " at demo.test"))]]
             [:testcase {:name "demo.test/output-test"
                         :classname "demo.test"
                         :time "0.000000"
                         :column 1
                         :line 7
                         :file "fixtures/kaocha-demo/demo/test.clj"}
              [:failure {:message "[:fail] expected: (= {:foo 1} {:foo 2}). actual: (not (= {:foo 1} {:foo 2}))"
                         :type "assertion failure: ="}
               (str "FAIL in demo.test/output-test (test.clj:13)\n"
                    "it outputs location metadata when configured\n"
                    "oops\n"
                    "Expected:\n"
                    "  {:foo 1}\n"
                    "Actual:\n"
                    "  {:foo -1 +2}\n"
                    "╭───── Test output ───────────────────────────────────────────────────────\n"
                    "│ this is on stdout\n"
                    "│ this is on stderr\n"
                    "╰─────────────────────────────────────────────────────────────────────────")]]
             [:testcase {:name "demo.test/pending-test"
                         :classname "demo.test"
                         :time "0.000000"
                         :line 26
                         :column 1}
              [:skipped]]
             [:testcase {:name "demo.test/skip-test"
                         :classname "demo.test"
                         :time "0.000000"}]
             [:system-out "this is on stdout\nthis is on stderr\n"]
             [:system-err]]]

           (-> {:tests [{:test-paths ["fixtures/kaocha-demo"]
                         :ns-patterns [".*"]}]
                :kaocha.plugin.randomize/randomize? false
                :kaocha.plugin.junit-xml/add-location-metadata? true
                :kaocha.plugin.junit-xml/use-relative-path-in-location? true
                :color? false
                :reporter identity}
               repl/config
               api/run
               junit-xml/result->xml
               xml->hiccup))))
    (testing "it renders start-time when present"
      (is (= [:testsuites
              [:testsuite {:name "my-test-type" :id 0 :hostname "localhost"
                           :package ""
                           :errors 0 :failures 1 :tests 1
                           :timestamp "2007-12-03T10:15:30" :time "0.000012"
                           :skipped 0}
               [:properties]
               [:testcase {:classname nil
                           :name "my-test-type"
                           :time "0.000012"}]
               [:system-out]
               [:system-err]]]
             (-> {:kaocha.result/tests [{:kaocha.testable/id :my-test-type
                                         :kaocha.testable/type ::foo
                                         :kaocha.result/count 1
                                         :kaocha.result/fail 1
                                         :kaocha.result/pass 1
                                         :kaocha.result/error 0
                                         :kaocha.plugin.profiling/start (java.time.Instant/parse "2007-12-03T10:15:30.00Z")
                                         :kaocha.plugin.profiling/duration 12345}]}
                 junit-xml/result->xml
                 xml->hiccup)))))


(deftest makeparent-test
  (junit-xml/mkparent (io/file "target/foo/bar/baz.xml"))
  (is (.isDirectory (io/file "target/foo/bar")))
  (.delete (io/file "target/foo/bar"))
  (.delete (io/file "target/foo")))

(deftest testcase->xml-test
  (is (match?
       [:testcase
        {:name "my.app/my-test-case", :classname "my.app", :time "0.000000"}
        [:failure
         {:message
          "[:fail] expected: (= {:foo 4} {:foo 5}). actual: (not (= {:foo 4} {:foo 5}))",
          :type "assertion failure: ="}
         "FAIL in  (bar.clj:108)\nExpected:\n  {:foo 4}\nActual:\n  {:foo -4 +5}"]
        [:error
         {:message "oh no", :type "java.lang.Exception"}
         #(str/starts-with? % "ERROR in  (foo.clj:42)\nException: java.lang.Exception: oh no\n at kaocha.plugin.junit_xml_test")]]

       (->> {:kaocha.testable/id :my.app/my-test-case
             :kaocha.result/error 1
             :kaocha.testable/events [{:file "bar.clj"
                                       :line 108
                                       :type :fail
                                       :expected '(= {:foo 4} {:foo 5}),
                                       :actual '(not (= {:foo 4} {:foo 5}))
                                       :message nil}
                                      {:file "foo.clj"
                                       :line 42
                                       :type :error
                                       :actual (Exception. "oh no")}]}
            (junit-xml/testcase->xml {})
            xml->hiccup))))

(deftest suite->xml-test
  (is (match? {:tag :testsuite
               :attrs {:errors 0
                       :package "foo"
                       :tests 0
                       :name "foo/bar"
                       :time "0.000000"
                       :hostname "localhost"
                       :id 0
                       :failures 0}
               :content
               [{:tag :properties}
                {:tag :system-out :content ()}
                {:tag :system-err}]}
              (junit-xml/suite->xml {} {:kaocha.testable/id :foo/bar} 0)))

  (is (match? {:tag :testsuite
               :attrs {:errors 0
                       :package ""
                       :tests 0
                       :name "foo"
                       :time "0.000000"
                       :hostname "localhost"
                       :id 0
                       :failures 0}
               :content
               [{:tag :properties}
                {:tag :system-out :content ()}
                {:tag :system-err}]}
              (junit-xml/suite->xml {} {:kaocha.testable/id :foo} 0)))

  (is (match? {:tag :testsuite
               :attrs {:errors 0
                       :package ""
                       :tests 0
                       :name "foo"
                       :time "0.000000"
                       :hostname "localhost"
                       :id 0
                       :failures 0}
               :content
               [{:tag :properties}
                {:tag :system-out}
                {:tag :system-err}]}
              (junit-xml/suite->xml {:no-system-out? true} {:kaocha.testable/id :foo} 0))))

(deftest junit-xml-plugin
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
      (is (= [:testsuites
              [:testsuite {:errors "0"
                           :tests "1"
                           :name "my-test-type"
                           :package ""
                           :time "0.000012"
                           :hostname "localhost"
                           :id "0"
                           :timestamp "2007-12-03T10:15:30"
                           :failures "0"
                           :skipped "0"}
               [:properties]
               [:system-out]
               [:system-err]]]
             (xml->hiccup (clojure.xml/parse (io/file (str outfile))
                                             (fn [s ch]
                                               (let [parser (.. SAXParserFactory (newInstance) (newSAXParser))]
                                                 ;; avoid reflection warnings
                                                 (.parse ^SAXParser parser
                                                         ^java.io.File s
                                                         ^org.xml.sax.helpers.DefaultHandler ch))))))))))

(deftest valid-xml-test
  (testing "the output conforms to the JUnit.xsd schema"
    (is (= {:valid? true}
           (-> {:tests [{:test-paths ["fixtures/kaocha-demo"]
                         :ns-patterns [".*"]}]
                :kaocha.plugin.randomize/randomize? false
                :color? false
                :reporter identity}
               repl/config
               api/run
               junit-xml/result->xml
               (xml/validate (io/resource "kaocha/junit_xml/JUnit.xsd")))))))
