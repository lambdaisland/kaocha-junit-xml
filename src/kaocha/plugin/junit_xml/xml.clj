(ns kaocha.plugin.junit-xml.xml
  (:require [clojure.string :as str]))

(def entities
  {"&" "&amp;"
   "'" "&#27;"
   "\"" "&quot;"
   "<" "&lt;"
   ">" "&gt;"})

(defn escape-text [s]
  (str/replace s #"[&'\"<>]" entities))

(defn escape-attr [s]
  (str/replace s #"[&\"]" entities))

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
