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
    (loop [oldval @this]
      (if (compare-and-set! this oldval newval)
        [oldval newval]
        (recur @this))))
  (compareAndSet [this oldval newval]
    (r/wcar conn (r/watch k))
    (if (not= oldval @this)
      (do (r/wcar conn (r/unwatch))
          false)
      (some? (r/wcar conn
                     (r/multi)
                     (r/set k {:data newval})
                     (r/exec)))))
  (swap [this f]
    (loop [oldval @this]
      (let [newval (f oldval)]
        (if (compare-and-set! this oldval newval)
          newval
          (recur @this)))))
  (swap [this f x]
    (loop [oldval @this]
      (let [newval (f oldval x)]
        (if (compare-and-set! this oldval newval)
          newval
          (recur @this)))))
  (swap [this f x y]
    (loop [oldval @this]
      (let [newval (f oldval x y)]
        (if (compare-and-set! this oldval newval)
          newval
          (recur @this)))))
  (swap [this f x y args]
    (loop [oldval @this]
      (let [newval (apply f oldval x y args)]
        (if (compare-and-set! this oldval newval)
          newval
          (recur @this)))))
  (swapVals [this f]
    (loop [oldval @this]
      (let [newval (f oldval)]
        (if (compare-and-set! this oldval newval)
          [oldval newval]
          (recur @this)))))
  (swapVals [this f x]
    (loop [oldval @this]
      (let [newval (f oldval x)]
        (if (compare-and-set! this oldval newval)
          [oldval newval]
          (recur @this)))))
  (swapVals [this f x y]
    (loop [oldval @this]
      (let [newval (f oldval x y)]
        (if (compare-and-set! this oldval newval)
          [oldval newval]
          (recur @this)))))
  (swapVals [this f x y args]
    (loop [oldval @this]
      (let [newval (apply f oldval x y args)]
        (if (compare-and-set! this oldval newval)
          [oldval newval]
          (recur @this))))))

(defn redis-atom
  [conn k val]
  (let [r-atom (RedisAtom. conn k)]
    (reset! r-atom val)
    r-atom))
