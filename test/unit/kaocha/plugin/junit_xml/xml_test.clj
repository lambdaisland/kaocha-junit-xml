(ns kaocha.plugin.junit-xml.xml-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer :all]
            [kaocha.plugin.junit-xml.xml :as xml]
            [matcher-combinators.test]
            [matcher-combinators.matchers :as m])
  (:import java.io.FileInputStream))

(deftest emit-test
  (testing "escapes text"
    (is (= "<?xml version='1.0' encoding='UTF-8'?>\n<foo>\n&lt;hello&gt; &amp; &lt;world&gt; are &quot;great&quot;, aren&apos;t they?\n</foo>\n"
           (xml/emit-str {:tag :foo
                          :content ["<hello> & <world> are \"great\", aren't they?"]}))))

  (testing "escapes attributes"
    (is (= "<?xml version='1.0' encoding='UTF-8'?>\n<foo hello=\"'twas brillig &amp; the &quot;slithy&quot; toves\"/>\n"
           (xml/emit-str {:tag :foo
                          :attrs {:hello "'twas brillig & the \"slithy\" toves"}})))))

(deftest validate-test
  (is (match? {:valid? false,
               :line-number 1,
               :column-number 6,
               :public-id nil,
               :system-id nil,
               :message (m/regex #"Cannot find the declaration of element 'foo'")}
              (xml/validate "<foo></foo>"
                            (io/resource "kaocha/junit_xml/JUnit.xsd")))))

(deftest coercions-test
  (let [schema-file (io/file (io/resource  "kaocha/junit_xml/JUnit.xsd"))
        schema (xml/xml-schema schema-file)]
    (is (= (xml/as-schema schema) schema))

    (is (instance? javax.xml.transform.stream.StreamSource (xml/as-source (FileInputStream. schema-file))))))
