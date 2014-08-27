(ns prone.prep-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer :all]
            [prone.debug :refer [debug]]
            [prone.prep :refer [prep-error-page prep-debug-page]])
  (:import [java.io ByteArrayInputStream]))

(defn prep-frames [frames & [application-name]]
  (-> (prep-error-page {:frames frames} {} {} application-name)
      :error :frames))

(deftest ids-for-frames
  (is (= [0 1 2] (map :id (prep-frames [{} {} {}])))))

(deftest source-for-frames
  (is (re-find #"prone.prep-test"
               (:code (:source (first (prep-frames [{:class-path-url "prone/prep_test.clj"}]))))))
  (is (= "(unknown source file)"
         (:failure (:source (first (prep-frames [{}]))))))
  (is (= "(could not locate source file on class path)"
         (:failure (:source (first (prep-frames [{:class-path-url "plone/plep_test.clj"}])))))))

(deftest application-frames
  (is (= ["a"] (->> (prep-frames [{:name "a" :package "prone.prep-test"}
                                  {:name "b" :package "plone.plep-test"}]
                                 "prone")
                    (filter :application?)
                    (map :name)))))

(deftest selection-for-first-frame
  (is (= ["a"] (->> (prep-frames [{:name "a"} {:name "b"} {:name "c"}])
                    (filter :selected?)
                    (map :name)))))

(deftest selection-for-first-application-frame
  (is (= ["b"] (->> (prep-frames [{:name "a", :package "core.main"}
                                  {:name "b", :package "prone.core"}
                                  {:name "c", :package "prone.plone"}] "prone")
                    (filter :selected?)
                    (map :name)))))

(deftest frame-filter
  (is (= :application (:frame-filter (prep-error-page {:frames []} {} {} "")))))

(deftest no-unreadable-forms
  (is (= {:name "John Doe"
          :age 37
          :url {:prone.prep/to-string "http://example.com"
                :prone.prep/original-type "java.net.URL"}
          :body {:prone.prep/to-string "Hello"
                 :prone.prep/original-type "java.io.ByteArrayInputStream"}
          :lazy '(2 3 4)}
         (-> (prep-error-page {} {} {:session {:name "John Doe"
                                               :age 37
                                               :url (java.net.URL. "http://example.com")
                                               :body (ByteArrayInputStream. (.getBytes "Hello"))
                                               :lazy (map inc [1 2 3])}} "")
             :request :session))))

(defn prep-debug [debug]
  (prep-debug-page debug {}))

(deftest prep-debug-auxilliary-info
  (let [file (.getPath (io/resource "prone/debug_test.clj"))]

    (is (= :clj (:lang (first (:debug-data (prep-debug [{:file-name ""}]))))))

    (is (:selected? (first (:debug-data (prep-debug [{:file-name ""}])))))

    (is (= "test/prone/debug_test.clj"
           (:file-name (first (:debug-data (prep-debug [{:file-name file}]))))))

    (is (= "prone.debug-test"
           (:package (first (:debug-data (prep-debug [{:file-name file}]))))))

    (let [source (:source (first (:debug-data (prep-debug [{:file-name file}]))))]
      (is (re-find #"^\(ns prone\.debug-test" (:code source)))
      (is (= 0 (:offset source))))))
