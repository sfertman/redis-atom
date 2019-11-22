(ns redis-atom.RedisAtom-test
  (:require
    [clojure.test :refer :all]
    [redis-atom.RedisAtom]))

(def conn {:a 1 :b 2 :c {:d 4}})

(deftest test-init
  (let [a (RedisAtom. conn :test-init)]
    (is (= {:conn conn :k :test-init} (.state a))))
  (let [a (RedisAtom. conn :test-init-with-meta {:hello "world"})]
    (is (= {:conn conn :k :test-init-with-meta} (.state a)))
    (is (= {:hello "world"} (meta a)))))
