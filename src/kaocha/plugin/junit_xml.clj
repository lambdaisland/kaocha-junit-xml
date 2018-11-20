(ns kaocha.plugin.junit-xml
  (:require [clojure.java.io :as io]
            [kaocha.core-ext :refer :all]
            [kaocha.hierarchy :as hierarchy]
            [kaocha.plugin :as plugin :refer [defplugin]]
            [kaocha.plugin.junit-xml.xml :as xml]
            [kaocha.report :as report]
            [kaocha.result :as result]
            [kaocha.testable :as testable]
            [clojure.string :as str])
  (:import java.time.Instant))

(defn inst->iso8601 [inst]
  (.. java.time.format.DateTimeFormatter/ISO_LOCAL_DATE_TIME
      (withZone (java.time.ZoneId/systemDefault))
      (format (.truncatedTo inst java.time.temporal.ChronoUnit/SECONDS))))

;; The ESC [ is followed by any number (including none) of "parameter bytes" in
;; the range 0x30–0x3F (ASCII 0–9:;<=>?), then by any number of "intermediate
;; bytes" in the range 0x20–0x2F (ASCII space and !"#$%&'()*+,-./), then finally
;; by a single "final byte" in the range 0x40–0x7E (ASCII @A–Z[\]^_`a–z{|}~).
(defn strip-ansi-sequences [s]
  (str/replace s #"\x1b\[[\x30-x3F]*[\x20-\x2F]*[\x40-\x7E]" ""))

(defn test-seq [testable]
  (cons testable (mapcat test-seq (::result/tests testable))))

(defn leaf-tests [testable]
  (filter #(isa? (::testable/type %) :kaocha.testable.type/leaf)
          (test-seq testable)))

(defn time-stat [testable]
  (let [duration (:kaocha.plugin.profiling/duration testable 0)]
    (when duration
      {:time (format "%.6f" (/ duration 1e9))})))

(defn stats [testable]
  (let [tests      (test-seq testable)
        totals     (result/totals (::result/tests testable))
        start-time (:kaocha.plugin.profiling/start testable (Instant/now))]
    {:errors   (::result/error totals)
     :failures (::result/fail totals)
     :tests    (::result/count totals)
     :timestamp (inst->iso8601 start-time)}))

(defn test-name [test]
  (let [id (::testable/id test)]
    (str (if-let [n (and (qualified-ident? id) (namespace id))]
           (str n "/"))
         (name id))))

(defn failure-message [events]
  (strip-ansi-sequences
   (with-out-str
     (run! report/print-expr events))))

(defn error-type [events]
  (let [e (first (filter #(throwable? (:actual %)) events))]
    (.getName (class (:actual e)))))

(defn testcase->xml [test]
  (let [{::testable/keys [id skip events]
         ::result/keys   [pass fail error]
         :or             {pass 0 fail 0 error 0}} test]
    {:tag     :testcase
     :attrs   (merge {:name       (test-name test)
                      :classname  (namespace id)}
                     (time-stat test))
     :content (keep identity
                    [(cond
                       (> error 0) (let [events (filter (comp #{:error} :type) events)
                                         message (some-> events first :actual (.getMessage))
                                         trace (failure-message events)]
                                     {:tag   :error
                                      :attrs {:message message
                                              :type    (error-type (::testable/events test))}
                                      :content [trace]})
                       (> fail 0)  {:tag   :failure
                                    :attrs {:message (->> events
                                                          (filter #(and (hierarchy/fail-type? %)
                                                                        (not (= :error (:type %)))))
                                                          failure-message)
                                            :type    (str (first (remove #{:default} (map report/assertion-type events))))}})
                     ])}))

(defn suite->xml [suite index]
  (let [id (::testable/id suite)]
    {:tag :testsuite
     :attrs (-> {:name (test-name suite)
                 :id index
                 :hostname "localhost"}
                (merge (stats suite) (time-stat suite))
                (assoc :package (if (qualified-ident? id)
                                  (namespace id)
                                  "")))
     :content (concat
               [{:tag :properties}]
               (map testcase->xml (leaf-tests suite))
               [{:tag :system-out
                 :content (->> suite
                               test-seq
                               (keep :kaocha.plugin.capture-output/output)
                               (remove #{""})
                               (map strip-ansi-sequences))}
                {:tag :system-err}])}))

(defn result->xml [result]
  (let [suites (::result/tests result)]
    {:tag     :testsuites
     :attrs   {}
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
