(ns redis-atom.core
  (:require
    [taoensso.carmine :as r]))

(gen-class
  :name "RedisAtom"
  :extends clojure.lang.ARef
  :implements [clojure.lang.IDeref clojure.lang.IAtom2]
  :state "state"
  :init "init"
  :constructors {[
    clojure.lang.PersistentArrayMap
    clojure.lang.Keyword
  ] []
  [
    clojure.lang.PersistentArrayMap
    clojure.lang.Keyword
    clojure.lang.PersistentArrayMap
  ] []
  [
    clojure.lang.PersistentArrayMap
    clojure.lang.Keyword
    clojure.lang.PersistentArrayMap
    clojure.lang.IPersistentMap
  ] [clojure.lang.IPersistentMap]})

(defn -init
  ([conn k] [[] {:conn conn :k k}])
  ([conn k opts] [[] {:conn conn :k k :options opts}])
  ([conn k opts mta] [[mta] {:conn conn :k k :options opts}]))

(defn- conn [^RedisAtom this] (:conn (.state this)))
(defn- k [^RedisAtom this] (:k (.state this)))

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

(defn -deref [this] (:data (r/wcar (conn this) (r/get (k this)))))

(defn -reset [this newval]
  (validate* (.getValidator this) newval)
  (let [oldval (.deref this)]
    (r/wcar (conn this) (r/set (k this) {:data newval}))
    (.notifyWatches this oldval newval)
    newval))

(defn -resetVals [this newval]
  (loop [oldval (.deref this)]
    (if (.compareAndSet this oldval newval)
      [oldval newval]
      (recur (.deref this)))))

(defn- compareAndSet* [this oldval newval]
  (let [conn* (conn this)
        k* (k this)]
    (r/wcar conn* (r/watch k*))
    (if (not= oldval (.deref this))
      (do (r/wcar conn* (r/unwatch))
          false)
      (some? (r/wcar conn*
                     (r/multi)
                     (r/set k* {:data newval})
                     (r/exec))))))

(defn -compareAndSet [this oldval newval]
  (validate* (.getValidator this) newval)
  (if (compareAndSet* this oldval newval)
    (do (.notifyWatches this oldval newval)
        true)
    false))

(defn -swap-IFn [this f]
  (loop [oldval (.deref this)]
    (let [newval (f oldval)]
      (if (.compareAndSet this oldval newval)
        newval
        (recur (.deref this))))))
(defn -swap-IFn-Object [this f x]
  (loop [oldval (.deref this)]
    (let [newval (f oldval x)]
      (if (.compareAndSet this oldval newval)
        newval
        (recur (.deref this))))))
(defn -swap-IFn-Object-Object [this f x y]
  (loop [oldval (.deref this)]
    (let [newval (f oldval x y)]
      (if (.compareAndSet this oldval newval)
        newval
        (recur (.deref this))))))
(defn -swap-IFn-Object-Object-ISeq [this f x y args]
  (loop [oldval (.deref this)]
    (let [newval (apply f oldval x y args)]
      (if (.compareAndSet this oldval newval)
        newval
        (recur (.deref this))))))

(defn -swapVals-IFn [this f]
  (loop [oldval (.deref this)]
    (let [newval (f oldval)]
      (if (.compareAndSet this oldval newval)
        [oldval newval]
        (recur (.deref this))))))
(defn -swapVals-IFn-Object [this f x]
  (loop [oldval (.deref this)]
    (let [newval (f oldval x)]
      (if (.compareAndSet this oldval newval)
        [oldval newval]
        (recur (.deref this))))))
(defn -swapVals-IFn-Object-Object [this f x y]
  (loop [oldval (.deref this)]
    (let [newval (f oldval x y)]
      (if (.compareAndSet this oldval newval)
        [oldval newval]
        (recur (.deref this))))))
(defn -swapVals-IFn-Object-Object-ISeq [this f x y args]
  (loop [oldval (.deref this)]
    (let [newval (apply f oldval x y args)]
      (if (.compareAndSet this oldval newval)
        [oldval newval]
        (recur (.deref this))))))

(defn redis-atom
  ([conn k val] (let [a (RedisAtom. conn k)] (.reset a val) a))
  ([conn k val & {opts :options vtor :validator mta :meta}]
    (let [a (RedisAtom. conn k opts mta)]
      (.reset a val)
      (when vtor (.setValidator a vtor))
      a)))
