(ns redis-atom.core-test
  (:require
    [clojure.test :refer :all]
    [redis-atom.core :as r]
    [taoensso.carmine :as redis]))


(def conn {
  :pool {}
  :spec {:uri "redis://localhost:6379"}})

(defmacro wcar* [& body] `(redis/wcar conn ~@body))

(wcar* (redis/flushall))

(deftest test-atom
  (let [a (r/atom conn :test-atom 42)]
    (is (= {:conn conn :key :test-atom} a))
    (is (= {:data 42} (wcar* (redis/get :test-atom))))))

(deftest test-deref
  (let [a (r/atom conn :test-deref 42)]
    (is (= 42 (r/deref a)))))

(deftest test-reset
  (let [a (r/atom conn :test-reset 42)]
    (is (= 42 (r/deref a)))
    (is (= 44 (r/reset! a 44)))
    (is (= 44 (r/deref a)))))

(deftest test-compare-and-set
  (let [a (r/atom conn :test-compare-and-set 42)]
    (is (= 42 (r/deref a)))
    (is (false? (r/compare-and-set! a 57 44)))
    (is (= 42 (r/deref a)))
    (is (true? (r/compare-and-set! a 42 44)))
    (is (= 44 (r/deref a)))))

(deftest test-swap
  (let [a (r/atom conn :test-swap 42)]
    (is (= 42 (r/deref a)))))
    ;; ^ this is where tests get fun --- continue here
