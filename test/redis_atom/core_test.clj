(ns redis-atom.core-test
  (:require
    [clojure.test :refer :all]
    [redis-atom.core :refer [redis-atom]]
    [taoensso.carmine :as redis])
  (:import
    java.lang.IllegalStateException))

(def conn {:pool {} :spec {:uri "redis://localhost:6379"}})

(defmacro wcar* [& body] `(redis/wcar conn ~@body))

(wcar* (redis/flushall))

(deftest test-create
  (testing "create with conn & k *without* existing key on backend"
    (let [a (redis-atom conn :test-create-1)
          state-a (.state a)]
      (is (= conn (:conn state-a)))
      (is (= :test-create-1 (:k state-a)))
      (is (nil? @a))))
  (testing "create with conn & k *with* existing key on backend"
    (let [_ (redis-atom conn :test-create-2 42)
          a (redis-atom conn :test-create-2)
          state-a (.state a)]
    (is (= conn (:conn state-a)))
    (is (= :test-create-2 (:k state-a)))
    (is (= 42 @a))))
  (testing "create with conn, k & val *without* existing key on backend"
    (let [a (redis-atom conn :test-create-3 42)
          state-a (.state a)]
      (is (= conn (:conn state-a)))
      (is (= :test-create-3 (:k state-a)))
      (is (= 42 @a))))
  (testing "create with conn, k & val *with* existing key on backend"
    (let [_ (redis-atom conn :test-create-4 42)
          a (redis-atom conn :test-create-4 43)
          state-a (.state a)]
      (is (= conn (:conn state-a)))
      (is (= :test-create-4 (:k state-a)))
      (is (= 42 @a)))))

(deftest test-deref
  (let [a (redis-atom conn :test-deref 42)]
    (is (= 42 @a))))

(deftest test-reset
  (let [a (redis-atom conn :test-reset 42)]
    (is (= 42 @a))
    (is (= 44 (reset! a 44)))
    (is (= 44 @a))))

(deftest test-reset-vals
  (let [a (redis-atom conn :test-reset-val 42)]
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
    (is (= 44 @a)))
  (testing "compare and set will properly return false if there is a modification while executing redis transaction"
    ;; NOTE this code tries to trigger a race condition, but it may be flaky
    ;; depending on how the threads are started.
    (let [test-key
          :test-compare-and-set]
      (try
        (let [increment-key
              (fn increment-key []
                (loop [i 100000]
                  (when (pos? i)
                    (do (redis/wcar conn
                                    (redis/set test-key i))
                        (recur (dec i))))))

              watch-and-transact
              (fn watch-and-transact []
                (redis/wcar conn
                            (redis/watch test-key))
                (let [result
                      (redis/wcar conn
                                  (redis/multi)
                                  (redis/set test-key 0)
                                  (redis/exec))]
                  (= ["OK"] (last result))))

              inc-future
              (future (increment-key))

              watch-future
              (future (watch-and-transact))

              _inc-res @inc-future
              watch-res @watch-future]

          (is (= false watch-res)))
        (finally
          (redis/wcar conn
                      (redis/del test-key)))))))

(defn wait-and-inc
  [t-ms x]
  (Thread/sleep t-ms)
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
  (let [a (redis-atom conn :test-swap-vals-arity 42)]
    (is (= [42 43] (swap-vals! a inc)))
    (is (= [43 44] (swap-vals! a + 1)))
    (is (= [44 46] (swap-vals! a + 1 1)))
    (is (= [46 49] (swap-vals! a + 1 1 1)))
    (is (= [49 53] (swap-vals! a + 1 1 1 1)))))

(deftest test-swap-locking
  (let [a (redis-atom conn :test-swap-locking 42)
        b (redis-atom conn :test-swap-locking)]
    ;; NOTE here we had to change the result, since now the first thread will
    ;; complete first, no matter if it takes longer than the second. To achieve
    ;; the original effect, the thread sleep should be outside of the swap! (see
    ;; testing below)
    (future
      (is (= 43 (swap! a (partial wait-and-inc 100)))))
    (future
      (is (= 44 (swap! b (partial wait-and-inc  50)))))
    (Thread/sleep 250))

  (testing "Waiting outside of swap"
    (let [a (redis-atom conn :test-swap-locking-2 42)
          b (redis-atom conn :test-swap-locking-2)]
      (future
        (Thread/sleep 100)
        (is (= 44 (swap! a inc))))
      (future
        (Thread/sleep 50)
        (is (= 43 (swap! b inc))))
      (Thread/sleep 250)))

  (testing "Lock is freed even when function crashes"
    (let [a (redis-atom conn :test-swap-locking-3 42)
          b (redis-atom conn :test-swap-locking-3)
          crash-me (fn [_] (/ 1 0))]
      (future
        (Thread/sleep 100)
        (is (= 43 (swap! a inc))))
      (future
        (Thread/sleep 50)
        (is (thrown? java.lang.ArithmeticException (swap! b crash-me))))
      (Thread/sleep 250))))

(deftest test-swap-vals-locking
  (let [a (redis-atom conn :test-swap-vals-locking 42)
        b (redis-atom conn :test-swap-vals-locking)
        results (volatile! #{})]
    ;; NOTE here we had to change the result, since now the first thread will
    ;; complete first, no matter if it takes longer than the second. To achieve
    ;; the original effect, the thread sleep should be outside of the swap! (see
    ;; testing below)
    (future
      (let [new-vals
            (swap-vals! a (partial wait-and-inc 100))]
        (is (vswap! results conj new-vals))))
    (Thread/sleep 10)
    (future
      (let [new-vals
            (swap-vals! b (partial wait-and-inc 50))]
        (is (vswap! results conj new-vals))))
    (Thread/sleep 250)
    (is (= #{[42 43] [43 44]} @results)))

  (testing "Waiting outside of swap"
    (let [a (redis-atom conn :test-swap-vals-locking-2 42)
          b (redis-atom conn :test-swap-vals-locking-2)]
    ;; NOTE here we had to change the result, since now the first thread will
    ;; complete first, no matter if it takes longer than the second. To achieve
    ;; the original effect, the thread sleep should be outside of the swap! (see
    ;; testing below)
      (future
        (Thread/sleep 100)
        (is (= [43 44] (swap-vals! a inc))))
      (future
        (Thread/sleep 50)
        (is (= [42 43] (swap-vals! b inc))))
      (Thread/sleep 250))))

(deftest test-watches
  (let [a (redis-atom conn :test-watches 42)
        watcher-atom (atom nil)]
    (add-watch a :watcher (fn [& args] (reset! watcher-atom args)))
    (reset! a 43)
    (is (= 43 @a))
    (is (= @watcher-atom [:watcher a 42 43]))
    (remove-watch a :watcher)
    (reset! a 44)
    (is (= 44 @a))
    (is (= @watcher-atom [:watcher a 42 43]))))

(defmacro try-catch-invalid-state [form]
  `(try ~form
        (is (= 0 1))
        (catch IllegalStateException e#
          (is (= "Invalid reference state" (.getMessage e#))))))

(deftest test-validator
  (let [a (redis-atom conn :test-valdator 42 :validator (fn [newval] (< newval 43)))]
    (is (= 42 @a))
    (try-catch-invalid-state (reset! a 43))
    (try-catch-invalid-state (reset-vals! a 43))
    (try-catch-invalid-state (swap! a inc))
    (try-catch-invalid-state (swap! a + 1))
    (try-catch-invalid-state (swap! a + 1 1))
    (try-catch-invalid-state (swap! a + 1 1 1))
    (try-catch-invalid-state (swap! a + 1 1 1 1))
    (try-catch-invalid-state (swap-vals! a inc))
    (try-catch-invalid-state (swap-vals! a + 1))
    (try-catch-invalid-state (swap-vals! a + 1 1))
    (try-catch-invalid-state (swap-vals! a + 1 1 1))
    (try-catch-invalid-state (swap-vals! a + 1 1 1 1))
    (try-catch-invalid-state (compare-and-set! a 42 43))
    (try-catch-invalid-state (compare-and-set! a 43 44))))

(deftest test-meta
  (let [a (redis-atom conn :test-meta 42 :meta {:hello "meta"})]
    (is (= 42 @a))
    (is (= {:hello "meta"} (meta a)))))
