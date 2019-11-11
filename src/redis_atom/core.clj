(ns redis-atom.core
  (:require
    [taoensso.carmine :as r])
  (:import
    clojure.lang.IAtom2
    clojure.lang.IDeref))

(deftype RedisAtom [conn k]
  IDeref
  (deref [_] (:data (r/wcar conn (r/get k))))

  IAtom2
  (reset [_ newval]
    (r/wcar conn (r/set k {:data newval}))
    newval)
  (resetVals [this newval]
    (loop [oldval (.deref this)]
      (if (.compareAndSet this oldval newval)
        [oldval newval]
        (recur (.deref this)))))
  (compareAndSet [this oldval newval]
    (r/wcar conn (r/watch k))
    (if (not= oldval (.deref this))
      (do (r/wcar conn (r/unwatch))
          false)
      (some? (r/wcar conn
                     (r/multi)
                     (r/set k {:data newval})
                     (r/exec)))))
  (swap [this f]
    (loop [oldval (.deref this)]
      (let [newval (f oldval)]
        (if (.compareAndSet this oldval newval)
          newval
          (recur (.deref this))))))
  (swap [this f x]
    (loop [oldval (.deref this)]
      (let [newval (f oldval x)]
        (if (.compareAndSet this oldval newval)
          newval
          (recur (.deref this))))))
  (swap [this f x y]
    (loop [oldval (.deref this)]
      (let [newval (f oldval x y)]
        (if (.compareAndSet this oldval newval)
          newval
          (recur (.deref this))))))
  (swap [this f x y args]
    (loop [oldval (.deref this)]
      (let [newval (apply f oldval x y args)]
        (if (.compareAndSet this oldval newval)
          newval
          (recur (.deref this))))))
  (swapVals [this f]
    (loop [oldval (.deref this)]
      (let [newval (f oldval)]
        (if (.compareAndSet this oldval newval)
          [oldval newval]
          (recur (.deref this))))))
  (swapVals [this f x]
    (loop [oldval (.deref this)]
      (let [newval (f oldval x)]
        (if (.compareAndSet this oldval newval)
          [oldval newval]
          (recur (.deref this))))))
  (swapVals [this f x y]
    (loop [oldval (.deref this)]
      (let [newval (f oldval x y)]
        (if (.compareAndSet this oldval newval)
          [oldval newval]
          (recur (.deref this))))))
  (swapVals [this f x y args]
    (loop [oldval (.deref this)]
      (let [newval (apply f oldval x y args)]
        (if (.compareAndSet this oldval newval)
          [oldval newval]
          (recur (.deref this)))))))

(defn redis-atom
  [conn k val]
  (let [r-atom (RedisAtom. conn k)]
    (.reset r-atom val)
    r-atom))
