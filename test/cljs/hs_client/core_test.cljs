(ns hs-client.core-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [hs-client.core :as core]))

(deftest fake-test
  (testing "fake description"
    (is (= 1 2))))
