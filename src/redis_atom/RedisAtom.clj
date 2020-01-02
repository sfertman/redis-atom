(ns redis-atom.RedisAtom
  (:require [redis-atom.redis :as r])
  (:gen-class
    :name RedisAtom
    :extends clojure.lang.ARef
    :implements [clojure.lang.IDeref clojure.lang.IAtom2]
    :state state
    :init init
    :constructors {
      [clojure.lang.PersistentArrayMap clojure.lang.Keyword] []
      [clojure.lang.PersistentArrayMap clojure.lang.Keyword clojure.lang.IPersistentMap] [clojure.lang.IPersistentMap]}))

(defn -init
  ([conn k] [[] {:conn conn :k k}])
  ([conn k mta] [[mta] {:conn conn :k k}]))

(defn- validate*
  "This is a clojure re-implementation of clojure.lang.ARef/validate because
  cannot be accessed by subclasses Needed to invoke when changing atom state"
  [^clojure.lang.IFn vf val]
  (try
    (if (and (some? vf) (not (vf val)))
      (throw (IllegalStateException. "Invalid reference state")))
    (catch RuntimeException re
      (throw re))
    (catch Exception e
      (throw (IllegalStateException. "Invalid reference state" e)))))

(defn -deref [this]
  (r/deref* (:conn (.state this)) (:k (.state this))))

(defn -reset [this newval]
  (validate* (.getValidator this) newval)
  (let [oldval (.deref this)]
    (r/reset* (:conn (.state this)) (:k (.state this)) newval)
    (.notifyWatches this oldval newval)
    newval))
;; ^^ Note: this looks dubious but this is the way clojure atom works at the moment. Seems like there's no point ensuring atomic tx here since there are explicit tools for that, namely swap!.

(defn -compareAndSet [this oldval newval]
  (validate* (.getValidator this) newval)
  (if (r/compare-and-set* (:conn (.state this)) (:k (.state this)) oldval newval)
    (do
      (.notifyWatches this oldval newval)
      true)
    false))

(defn -resetVals [this newval]
  (loop [oldval (.deref this)]
    (if (.compareAndSet this oldval newval)
      [oldval newval]
      (recur (.deref this)))))

(defn- swap*
  [this f & args]
  (loop [oldval (.deref this)]
    (let [newval (apply f oldval args)]
      (if (.compareAndSet this oldval newval)
        newval
        (recur (.deref this))))))

(defn -swap-IFn
  [this f] (swap* this f))
(defn -swap-IFn-Object
  [this f x] (swap* this f x))
(defn -swap-IFn-Object-Object
  [this f x y] (swap* this f x y))
(defn -swap-IFn-Object-Object-ISeq
  [this f x y args] (apply swap* this f x y args))

(defn- swap-vals*
  [this f & args]
  (loop [oldval (.deref this)]
    (let [newval (apply f oldval args)]
      (if (.compareAndSet this oldval newval)
        [oldval newval]
        (recur (.deref this))))))

(defn -swapVals-IFn
  [this f] (swap-vals* this f))
(defn -swapVals-IFn-Object
  [this f x] (swap-vals* this f x))
(defn -swapVals-IFn-Object-Object
  [this f x y] (swap-vals* this f x y))
(defn -swapVals-IFn-Object-Object-ISeq
  [this f x y args] (apply swap-vals* this f x y args))