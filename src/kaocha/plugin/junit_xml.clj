(ns kaocha.plugin.junit-xml
  (:refer-clojure :exclude [symbol])
  (:require [clojure.java.io :as io]
            [kaocha.core-ext :refer :all]
            [kaocha.hierarchy :as hierarchy]
            [kaocha.plugin :as plugin :refer [defplugin]]
            [kaocha.plugin.junit-xml.xml :as xml]
            [kaocha.report :as report]
            [kaocha.result :as result]
            [kaocha.testable :as testable]
            [clojure.string :as str])
  (:import java.time.Instant
           java.nio.file.Files
           java.nio.file.Paths
           java.nio.file.attribute.FileAttribute))

(defn inst->iso8601 [inst]
  (.. java.time.format.DateTimeFormatter/ISO_LOCAL_DATE_TIME
      (withZone (java.time.ZoneId/systemDefault))
      (format (.truncatedTo inst java.time.temporal.ChronoUnit/SECONDS))))

;; The ESC [ is followed by any number (including none) of "parameter bytes" in
;; the range 0x30–0x3F (ASCII 0–9:;<=>?), then by any number of "intermediate
;; bytes" in the range 0x20–0x2F (ASCII space and !"#$%&'()*+,-./), then finally
;; by a single "final byte" in the range 0x40–0x7E (ASCII @A–Z[\]^_`a–z{|}~).
(defn strip-ansi-sequences [s]
  (str/replace s #"\x1b\[[\x30-\x3F]*[\x20-\x2F]*[\x40-\x7E]" ""))

(defn test-seq [testable]
  (cons testable (mapcat test-seq (::result/tests testable))))

(defn leaf-tests [testable]
  (filter #(or (hierarchy/leaf? %)
               (result/failed-one? %))
          (test-seq testable)))

(defn time-stat [testable]
  (let [duration (:kaocha.plugin.profiling/duration testable 0)]
    (when duration
      {:time (format "%.6f" (/ duration 1e9))})))

(defn stats [testable]
  (let [tests      (test-seq testable)
        totals     (result/testable-totals testable)
        start-time (:kaocha.plugin.profiling/start testable (Instant/now))]
    {:errors   (::result/error totals)
     :failures (::result/fail totals)
     :skipped  (::result/pending totals)
     :tests    (::result/count totals)
     :timestamp (inst->iso8601 start-time)}))

(defn test-name [test]
  (let [id (::testable/id test)]
    (str (if-let [n (and (qualified-ident? id) (namespace id))]
           (str n "/"))
         (name id))))

(defn failure-message [m]
  (str/trim
   (strip-ansi-sequences
    (with-out-str
      (report/fail-summary m)))))

(defn classname [obj]
  (.getName (class obj)))

(defn skipped->xml [m]
  {:tag :skipped})

(defn failure->xml [m]
  (let [assertion-type (report/assertion-type m)]
    {:tag   :failure
     :attrs {:message (format "[%s] expected: %s. actual: %s"
                              (:type m)
                              (:expected m)
                              (:actual m))
             :type    (str "assertion failure"
                           (when-not (= :default assertion-type)
                             (str ": " assertion-type)))}
     :content [(failure-message m)]}))

(defn error->xml [m]
  (let [exception (when (throwable? (:actual m))
                    (:actual m))]
    {:tag   :error
     :attrs {:message (or (:message m)
                          (when exception
                            (.getMessage exception)))
             :type    (if exception
                        (classname exception)
                        ":error")}
     :content [(failure-message m)]}))

(defn absolute-path
  [test-file]
  (some-> test-file
          ;; TODO:
          ;; This works, but it is preferable to fix this upstream in `kaocha` so
          ;; we don't have to supply the classloader explicitly.
          ;; See the discussion on the following PR:
          ;; https://github.com/lambdaisland/kaocha-junit-xml/pull/13
          (io/resource (deref Compiler/LOADER))
          io/file
          .getAbsolutePath))

(defn relative-path
  [test-file]
  (when-let [absolute-path (absolute-path test-file)]
    (let [empty-string-array (make-array String 0)
          current-path       ^java.nio.file.Path (Paths/get (System/getProperty "user.dir") empty-string-array)
          file-path          ^java.nio.file.Path (Paths/get absolute-path empty-string-array)]
      (str (.relativize current-path file-path)))))

(defn set-file-path
  [m result]
  (if (::use-relative-path-in-location? result)
    (relative-path m)
    (absolute-path m)))

(defn test-location-metadata
  [m result]
  (-> m
      (get ::testable/meta)
      (select-keys [:line :column :file])
      (update :file set-file-path result)))

(defn testcase->xml [result test]
  (let [{::testable/keys [id skip events]
         ::result/keys   [pass fail error]
         :or             {pass 0 fail 0 error 0}} test]
    {:tag     :testcase
     :attrs   (merge {:name       (test-name test)
                      :classname  (namespace id)}
                     (time-stat test)
                     (when (some-> result ::add-location-metadata?)
                       (test-location-metadata test result)))
     :content (keep (fn [m]
                      (cond
                        (hierarchy/pending? m) (skipped->xml m)
                        (hierarchy/error-type? m) (error->xml m)
                        (hierarchy/fail-type? m) (failure->xml m)))
                    events)}))

(defn suite->xml [{::keys [omit-system-out?] :as result} suite index]
  (let [id (::testable/id suite)]
    {:tag     :testsuite
     :attrs   (-> {:name     (test-name suite)
                   :id       index
                   :hostname "localhost"}
                  (merge (stats suite) (time-stat suite))
                  (assoc :package (if (qualified-ident? id)
                                    (namespace id)
                                    "")))
     :content (concat
               [{:tag :properties}]
               (map (partial testcase->xml result) (leaf-tests suite))
               [(cond-> {:tag :system-out}
                  (not omit-system-out?)
                  (assoc :content (->> suite
                                       test-seq
                                       (keep :kaocha.plugin.capture-output/output)
                                       (remove #{""})
                                       (map strip-ansi-sequences))))
                {:tag :system-err}])}))

(defn result->xml [result]
  (let [suites (::result/tests result)]
    {:tag     :testsuites
     :attrs   {}
     :content (map (partial suite->xml result)
                   suites
                   (range))}))

(defn mkparent [path]
  (when-let [parent (.getParent path) ]
    (let [ppath (.toPath (io/file parent))]
      (Files/createDirectories ppath (into-array FileAttribute [])))))

(defn write-junit-xml [filename result]
  (let [file (io/file filename)]
    (mkparent file)
    (with-open [f (io/writer file)]
      (binding [*out* f]
        (xml/emit (result->xml result))))))

(defplugin kaocha.plugin/junit-xml
  "Write test results to junit.xml"

  (cli-options [opts]
    (conj opts
          [nil
           "--junit-xml-file FILENAME"
           "Save the test results to a Ant JUnit XML file."]
          [nil
           "--junit-xml-omit-system-out"
           "Do not add captured output to junit.xml"]
          [nil
           "--junit-xml-add-location-metadata"
           "Add line, column, and file attributes to tests in junit.xml"]
          [nil
           "--junit-xml-use-relative-path-in-location"
           "Updates the file attributes printed by --junit-xml-add-location-metadata to use pathing relative to where kaocha was executed from"]))

  (config [config]
    (let [target-file      (get-in config [:kaocha/cli-options :junit-xml-file])
          omit-system-out? (get-in config [:kaocha/cli-options :junit-xml-omit-system-out])
          add-location-metadata? (get-in config [:kaocha/cli-options :junit-xml-add-location-metadata])
          use-relative-path-in-location? (get-in config [:kaocha/cli-options :junit-xml-use-relative-path-in-location])]
      (cond-> config

        target-file
        (assoc ::target-file target-file)

        omit-system-out?
        (assoc ::omit-system-out? true)

        add-location-metadata?
        (assoc ::add-location-metadata? true)

        use-relative-path-in-location?
        (assoc ::use-relative-path-in-location? true))))

  (post-run [result]
    (when-let [filename (::target-file result)]
      (write-junit-xml filename result))
    result))
