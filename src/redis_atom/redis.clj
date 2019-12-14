(ns redis-atom.redis
  (:require [taoensso.carmine :as r]))

(defn- get-key
  "Gets hexified hashCode of this. Used internally as atom's key on redis"
  [this]
  (str "@" (Integer/toString (.hashCode this) 16)))

(defn deref*
  [this] (:data (r/wcar (:conn (.state this)) (r/get (get-key this)))))

(defn reset* [this newval]
  (let [oldval (.deref this)]
    (r/wcar (:conn (.state this)) (r/set (get-key this) {:data newval}))
    (.notifyWatches this oldval newval)
    newval))

(defn compare-and-set* [this oldval newval]
  (let [conn* (:conn (.state this))
        k* (get-key this)]
    (r/wcar conn* (r/watch k*))
    (if (not= oldval (.deref this))
      (do (r/wcar conn* (r/unwatch))
          false)
      (if (some? (r/wcar conn*
                         (r/multi)
                         (r/set k* {:data newval})
                         (r/exec)))
        (do (.notifyWatches this oldval newval)
            true)
        false))))
