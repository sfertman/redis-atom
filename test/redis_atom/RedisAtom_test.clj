(ns redis-atom.RedisAtom-test
  (:require
    [clojure.test :refer :all]
    [redis-atom.RedisAtom]))

(def conn {:a 1 :b 2 :c {:d 4}})

(deftest test-init
  (let [a (RedisAtom. conn)]
    (is (= {:conn conn} (.state a))))
  (let [a (RedisAtom. conn {:hello "world"})]
    (is (= {:conn conn} (.state a)))
    (is (= {:hello "world"} (meta a)))))
