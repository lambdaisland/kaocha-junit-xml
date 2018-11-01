(ns kaocha.plugin.junit-xml
  (:require [kaocha.plugin :as plugin :refer [defplugin]]
            [kaocha.core-ext :refer :all]
            [kaocha.testable :as testable]
            [kaocha.history :as history]
            [kaocha.result :as result]
            [clojure.xml :as xml]
            [clojure.java.io :as io]
            [kaocha.report :as report]
            [kaocha.hierarchy :as hierarchy]
            [kaocha.output :as output]))

(defn inst->iso8601 [inst]
  (.. java.time.format.DateTimeFormatter/ISO_LOCAL_DATE_TIME
      (withZone (java.time.ZoneId/systemDefault))
      (format (.truncatedTo inst java.time.temporal.ChronoUnit/SECONDS))))

(defn test-seq [testable]
  (cons testable (mapcat test-seq (::result/tests testable))))

(defn leaf-tests [testable]
  (filter #(isa? (::testable/type %) :kaocha.testable.type/leaf)
          (test-seq testable)))

(defn time-stat [testable]
  (let [duration (:kaocha.plugin.profiling/duration testable)]
    (when duration
      {:time (format "%.6f" (/ duration 1e9))})))

(defn stats [testable]
  (let [tests      (test-seq testable)
        totals     (result/totals (::result/tests testable))
        start-time (:kaocha.plugin.profiling/start testable)]
    (cond-> {:skipped (count (filter ::testable/skip tests))
             :errors   (::result/error totals)
             :failures (::result/fail totals)
             :tests    (::result/count totals)}
      start-time (assoc :timestamp (inst->iso8601 start-time)))))

(defn test-name [test]
  (let [id (::testable/id test)]
    (str (if-let [n (and (qualified-ident? id) (namespace id))]
           (str n "/"))
         (name id))))

(defn failure-message [events]
  (with-out-str
    (run! report/print-expr events)))

(defn error-type [events]
  (let [e (first (filter #(throwable? (:actual %)) events))]
    (.getName (class (:actual e)))))

(defn testcase->xml [test]
  (let [{::testable/keys [id skip events]
         ::result/keys   [pass fail error]
         :or             {pass 0 fail 0 error 0}} test]
    {:tag     :testcase
     :attrs   (merge {:name       (test-name test)
                      :classname  (namespace id)
                      :assertions (+ pass fail error)}
                     (time-stat test))
     :content (keep identity
                    [(cond
                       skip        {:tag :skipped}
                       (> error 0) {:tag   :error
                                    :attrs {:message (->> events
                                                          (filter (comp #{:error} :type))
                                                          failure-message)
                                            :type    (error-type (::testable/events test))}}
                       (> fail 0)  {:tag   :failure
                                    :attrs {:message (->> events
                                                          (filter #(and (hierarchy/fail-type? %)
                                                                        (not (= :error (:type %)))))
                                                          failure-message)
                                            :type    (first (remove #{:default} (map report/assertion-type events)))}})
                     (when-let [out (:kaocha.plugin.capture-output/output test)]
                       (when (seq out)
                         {:tag     :system-out
                          :content [out]}))])}))

(defn suite->xml [suite index]
  (let [id (::testable/id suite)]
    {:tag :testsuite
     :attrs (-> {:name (test-name suite)
                 :id index
                 :hostname "localhost"}
                (merge (stats suite) (time-stat suite))
                (cond-> (qualified-ident? id) (assoc :package (namespace id))))
     :content (map testcase->xml (leaf-tests suite))}))


(defn result->xml [result]
  (let [suites (::result/tests result)]
    {:tag     :testuites
     :attrs   (stats result)
     :content (map suite->xml suites (range))}))

(defn write-junit-xml [filename result]
  (with-open [f (io/writer (io/file filename))]
    (binding [*out* f]
      (xml/emit (result->xml result)))))

(defplugin kaocha.plugin/junit-xml
  "Write test results to junit.xml"

  (cli-options [opts]
    (conj opts [nil "--junit-xml-file FILENAME" "Save the test results to a Ant JUnit XML file."]))

  (config [config]
    (if-let [target (get-in config [:kaocha/cli-options :junit-xml-file])]
      (assoc config ::target-file target)
      config))

  (post-run [result]
    (when-let [filename (::target-file result)]
      (write-junit-xml filename result))
    result))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  (require 'kaocha.repl)
  (require 'kaocha.api)

  (def result
    (-> (kaocha.repl/config)

        (kaocha.api/run)))


  (xml/emit (result->xml result)

            )

  (def testable (first (filter (comp #{:kaocha.result-test/totals-test} ::testable/id) (test-seq result))))

  (let [events (::testable/events testable)]
    (filter events))

  (mapcat ::result/tests
          (::result/tests result))

  (count (leaf-tests (first (::result/tests result)))))
