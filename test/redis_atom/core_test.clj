(ns redis-atom.core-test
  (:require
    [clojure.core.async :refer [<!! timeout]]
    [clojure.test :refer :all]
    [redis-atom.core :refer [redis-atom]]
    [taoensso.carmine :as redis]))

(def conn {
  :pool {}
  :spec {:uri "redis://localhost:6379"}})

(defmacro wcar* [& body] `(redis/wcar conn ~@body))

(wcar* (redis/flushall))

(deftest test-atom
  (let [a (redis-atom conn :test-atom 42)]
    (is (= (.conn a) conn))
    (is (= (.k a) :test-atom))))

(deftest test-deref
  (let [a (redis-atom conn :test-deref 42)]
    (is (= 42 @a))))

(deftest test-reset
  (let [a (redis-atom conn :test-reset 42)]
    (is (= 42 @a))
    (is (= 44 (reset! a 44)))
    (is (= 44 @a))))

(deftest test-compare-and-set
  (let [a (redis-atom conn :test-compare-and-set 42)]
    (is (= 42 @a))
    (is (false? (compare-and-set! a 57 44)))
    (is (= 42 @a))
    (is (true? (compare-and-set! a 42 44)))
    (is (= 44 @a))))

(defn wait-and-inc
  [t-ms x]
  (<!! (timeout t-ms))
  (let [xpp (inc x)]
    (prn (str "wait-and-inc waited for " t-ms " [ms]: " x " + 1 = " xpp ))
    xpp))

(deftest test-swap
  (let [a (redis-atom conn :test-swap 42)]
    (is (= 42 @a))
    (swap! a inc)
    (is (= 43 @a))
    (future
      (is (= 45 (swap! a (partial wait-and-inc 1000)))))
    (future
      (is (= 44 (swap! a (partial wait-and-inc  500)))))
    (<!! (timeout 2500))))
