(ns redis-atom.core
  (:require
    [taoensso.carmine :as r]))

(defn- compare-and-set-key!
  "compare-and-set! without carmine connection context. Atomically sets the value of atom to newval if and only if the
  current value of the atom is identical to oldval. Returns true if
  set happened, else false"
  [k oldval newval]
  (r/watch k)
  (if (not= oldval (r/get k))
    (do
      (r/unwatch)
      false)
    (do
      (r/multi)
      (r/set k newval)
      (some? (r/exec))))) ;; exec will return nil if failed

(defn compare-and-set!
  "Atomically sets the value of atom to newval if and only if the
  current value of the atom is identical to oldval. Returns true if
  set happened, else false"
  [{:keys [conn k]} oldval newval]
  (r/wcar conn (compare-and-set-key! k oldval newval)))

(defn atom
  [conn k val]
  (r/wcar conn (r/set k val)) ;; need to check for success
  {:conn conn :key k})

(defn reset!
  "Sets the value of atom to newval without regard for the
  current value. Returns newval."
  [{:keys [conn k]} newval]
  (r/wcar conn (r/set k newval)) ;; need to check for sucess;
  newval)

(defn deref
  [{:keys [conn k]}]
  (r/wcar conn (r/get k)))

(defn swap!
  "Atomically swaps the value of atom to be:
  (apply f current-value-of-atom args). Note that f may be called
  multiple times, and thus should be free of side effects.  Returns
  the value that was swapped in."
  [{:keys [conn k]} f & args]
  (r/wcar
    conn
    (loop [oldval (r/get k)]
      (let [newval (apply f oldval args)]
        (if (compare-and-set-key! k oldval newval)
          newval
          (recur (r/get k)))))))
