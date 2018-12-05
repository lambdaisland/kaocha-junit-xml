(ns demo.test
  (:require [clojure.test :refer :all]))

(deftest basic-test
  (is (= {:foo 1} {:foo 1}) "at least one that passes"))

(deftest output-test
  (println "this is on stdout")

  (binding [*out* *err*]
    (println "this is on stderr"))

  (is (= {:foo 1} {:foo 2}) "oops"))

(deftest exception-in-is-test
  (is
   (throw (Exception. "Inside assertion"))))

(deftest exception-outside-is-test
  (throw (Exception. "outside assertion")))

(deftest ^:kaocha/skip skip-test
  (println "this test does not run.")
  (is false))
