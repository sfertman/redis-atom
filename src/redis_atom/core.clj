(ns redis-atom.core
  (:require
    [taoensso.carmine :as r]))

(defn atom
  "Creates and returns a redis-atom with connection spec
  conn, redis key k and value val"
  [conn k val]
  (r/wcar conn (r/set k {:data val}))
  {:conn conn :key k})

(defn deref
  [{:keys [conn key]}]
  (:data (r/wcar conn (r/get key))))

(defn reset!
  "Sets the value of atom to newval without regard for the
  current value. Returns newval."
  [{:keys [conn key]} newval]
  (r/wcar conn (r/set key {:data newval}))
  newval)

(defn compare-and-set!
  "Atomically sets the value of atom to newval if and only if the
  current value of the atom is identical to oldval. Returns true if
  set happened, else false"
  [{:keys [conn key] :as a} oldval newval]
  (r/wcar conn (r/watch key))
  (if (not= oldval (deref a))
    (do (r/wcar conn (r/unwatch))
        false)
    (some? (r/wcar conn
                   (r/multi)
                   (r/set key {:data newval})
                   (r/exec)))))

(defn swap!
  "Atomically swaps the value of atom to be:
  (apply f current-value-of-atom args). Note that f may be called
  multiple times, and thus should be free of side effects.  Returns
  the value that was swapped in."
  [a f & args]
  (loop [oldval (deref a)]
    (let [newval (apply f oldval args)]
      (if (compare-and-set! a oldval newval)
        newval
        (recur (deref a))))))
