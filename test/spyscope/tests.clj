(ns spyscope.tests
  (:require [clojure.test :refer :all]
            [puget.printer :as puget]
            [clojure.string :as str]
            [spyscope.core :refer [trace-storage]]
            [spyscope.repl :refer [trace-query trace-clear trace-next]]))

(use-fixtures :each (fn [t]
                      (trace-clear)
                      (await trace-storage)
                      (t)))

(set! *data-readers*
  {'spy/p spyscope.core/print-log
   'spy/d spyscope.core/print-log-detailed
   'spy/t spyscope.core/trace
   'spy/m spyscope.core/with-marker})

(deftest print-log-test
  (is (= (str (puget/cprint-str 6) "\n")
         (with-out-str #spy/p (+ 1 2 3)))))

(deftest print-log-detailed-test
  (is (re-matches #"(?s)spyscope\.tests.*\.invoke\(tests\.clj:.*\) \(\+ 1 2 3\) => .*"
                  (with-out-str #spy/d (+ 1 2 3))))
  (is (str/ends-with? (with-out-str #spy/d (+ 1 2 3))
                      (str (puget/cprint-str 6) "\n"))))

(deftest trace-test
  #spy/t (* 1 2 3)
  (await trace-storage)

  (is (re-matches #"(?s)spyscope\.tests.*\.invokeStatic\(tests\.clj:.*\) \(\* 1 2 3\) => .*"
                  (with-out-str (spyscope.repl/trace-query))))
  (is (str/ends-with? (with-out-str (spyscope.repl/trace-query))
                      (str (puget/cprint-str 6) "\n"))))

(deftest trace-can-keep-track-of-values
  #spy/t (* 2 3)
  (is (= 1 (count (trace-query {:print? false}))))
  (is (= 6 (-> (trace-query {:print? false}) first :value))))

(defn- echo [m]
  #spy/t m)

(deftest spy-m-for-filtering-sub-expressions
  #spy/m ^{:marker "a"} (echo 1)
  #spy/m ^{:marker "b"} (echo 2)
  (await trace-storage)

  (is (= [1] (map :value (trace-query {:print? false :marker "a"}))))
  (is (= [2] (map :value (trace-query {:print? false :marker "b"}))))
  (is (= [1 2] (map :value (trace-query {:print? false})))))
