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

(deftest test-reset-vals
  (let [a (redis-atom conn :test-reset 42)]
    (is (= 42 @a))
    (is (= [42 43] (reset-vals! a 43)))
    (is (= 43 @a))
    (is (= [43 44] (reset-vals! a 44)))
    (is (= 44 @a))
    (is (= [44 45] (reset-vals! a 45)))
    (is (= 45 @a))
    (is (= [45 "abc"] (reset-vals! a "abc")))
    (is (= "abc" @a))))

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

(deftest test-swap-arity
  (let [a (redis-atom conn :test-swap-arity 42)]
    (is (= 43 (swap! a inc)))
    (is (= 44 (swap! a + 1)))
    (is (= 46 (swap! a + 1 1)))
    (is (= 49 (swap! a + 1 1 1)))
    (is (= 53 (swap! a + 1 1 1 1)))))

(deftest test-swap-vals-arity
  (let [a (redis-atom conn :test-swap-arity 42)]
    (is (= [42 43] (swap-vals! a inc)))
    (is (= [43 44] (swap-vals! a + 1)))
    (is (= [44 46] (swap-vals! a + 1 1)))
    (is (= [46 49] (swap-vals! a + 1 1 1)))
    (is (= [49 53] (swap-vals! a + 1 1 1 1)))))

(deftest test-swap-locking
  (let [a (redis-atom conn :test-swap 42)]
    (future
      (is (= 44 (swap! a (partial wait-and-inc 100)))))
    (future
      (is (= 43 (swap! a (partial wait-and-inc  50)))))
    (<!! (timeout 250))))

(deftest test-swap-vals-locking
  (let [a (redis-atom conn :test-swap 42)]
    (future
      (is (= [43 44] (swap-vals! a (partial wait-and-inc 100)))))
    (future
      (is (= [42 43] (swap-vals! a (partial wait-and-inc  50)))))
    (<!! (timeout 250))))
