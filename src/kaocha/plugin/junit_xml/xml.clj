(ns kaocha.plugin.junit-xml.xml
  (:require [clojure.string :as str]
            [clojure.java.io :as io])
  (:import javax.xml.validation.SchemaFactory
           java.io.File
           javax.xml.validation.Schema
           javax.xml.validation.Validator
           javax.xml.transform.Source
           javax.xml.transform.stream.StreamSource
           java.io.StringReader))

(def entities
  {"&" "&amp;"
   "'" "&apos;"
   "\"" "&quot;"
   "<" "&lt;"
   ">" "&gt;"})

(defn escape-text [s]
  (-> s
      (or "")
      (str/replace #"[^\x09\x0A\x0D\x20-\xD7FF\xE000-\xFFFD]" "")
      (str/replace #"[&'\"<>]" entities)))

(defn escape-attr [s]
  (-> s
      (or "")
      (str/replace #"[^\x09\x0A\x0D\x20-\xD7FF\xE000-\xFFFD]" "")
      (str/replace #"[&\"]" entities)))

(defn emit-attr [[k v]]
  (print (str " " (name k) "=\"" (escape-attr v) "\"")))

(defn emit-element
  "Like xml/emit-element, but correctly escapes entities."
  [e]
  (if (instance? String e)
    (println (escape-text e))
    (do
      (print (str "<" (name (:tag e))))
      (run! emit-attr (:attrs e))
      (if (:content e)
	      (do
	        (println ">")
	        (run! emit-element (:content e))
	        (println (str "</" (name (:tag e)) ">")))
	      (println "/>")))))

(defn emit
  "Like xml/emit, but correctly escapes entities."
  [x]
  (println "<?xml version='1.0' encoding='UTF-8'?>")
  (emit-element x))

(defn emit-str [x]
  (with-out-str
    (emit x)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; validation

(def ^SchemaFactory schema-factory
  (memoize #(SchemaFactory/newInstance "http://www.w3.org/2001/XMLSchema")))

(defn ^Schema xml-schema [^File schema-file]
  (.newSchema (schema-factory) schema-file))

(defn ^Validator schema-validator [^Schema schema]
  (.newValidator schema))

(defprotocol SchemaCoercions
  (^Schema as-schema [_] "Coerce to javax.xml.validation.Schema")
  (^Validator as-validator [_] "Coerce to java.xml.validation.Validator"))

(defprotocol SourceCoercions
  (^Source as-source [_] "Coerce to java.xml.transform.Source"))

(extend-protocol SchemaCoercions
  Schema
  (as-schema [this] this)
  (as-validator [this] (schema-validator this))

  Object
  (as-schema [this] (xml-schema (io/as-file this)))
  (as-validator [this] (as-validator (as-schema this))))

(extend-protocol SourceCoercions
  Source
  (as-source [this] this)

  java.io.Reader
  (as-source [this] (as-source (StreamSource. this)))

  ;; xml source as string
  String
  (as-source [this] (as-source (StringReader. this)))

  ;; clojure.xml format
  clojure.lang.APersistentMap
  (as-source [this]
    (as-source (with-out-str (emit this))))

  ;; Anything that's coerible to a Reader
  Object
  (as-source [this] (as-source (io/reader this))))

(defn validate! [xml xsd]
  (.validate (as-validator xsd) (as-source xml)))

(defn validate [xml xsd]
  (try
    (validate! xml xsd)
    {:valid? true}
    (catch org.xml.sax.SAXParseException e
      {:valid? false
       :line-number (.getLineNumber e)
       :column-number (.getColumnNumber e)
       :public-id (.getPublicId e)
       :system-id (.getSystemId e)
       :message (.getMessage e)})))
