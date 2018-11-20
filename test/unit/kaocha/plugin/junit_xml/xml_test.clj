(ns kaocha.plugin.junit-xml.xml-test
  (:require [clojure.test :refer :all]
            [clojure.xml :as cxml]
            [kaocha.plugin.junit-xml.xml :as xml]
            [clojure.java.io :as io]))

(deftest emit-test
  (testing "escapes text"
    (is (= "<?xml version='1.0' encoding='UTF-8'?>\n<foo>\n&lt;hello&gt; &amp; &lt;world&gt; are &quot;great&quot;, aren&#27;t they?\n</foo>\n"
           (xml/emit-str {:tag :foo
                          :content ["<hello> & <world> are \"great\", aren't they?"]}))))

  (testing "escapes attributes"
    (is (= "<?xml version='1.0' encoding='UTF-8'?>\n<foo hello=\"'twas brillig &amp; the &quot;slithy&quot; toves\"/>\n"
           (xml/emit-str {:tag :foo
                          :attrs {:hello "'twas brillig & the \"slithy\" toves"}})))))

(deftest validate-test
  (is (= (xml/validate "<foo></foo>" (io/resource "kaocha/junit_xml/JUnit.xsd"))
         {:valid? false,
          :line-number 1,
          :column-number 6,
          :public-id nil,
          :system-id nil,
          :message "cvc-elt.1: Cannot find the declaration of element 'foo'."})))
