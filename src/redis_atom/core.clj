(ns redis-atom.core
  (:require
    [taoensso.carmine :as r]))

(def conn {})

(defn compare-and-set!
  "Atomically sets the value of atom to newval if and only if the
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

(defn atom
  [k val]
  (r/wcar conn (r/set k val)) ;; need to check for success
  k)

(defn reset!
  "Sets the value of atom to newval without regard for the
  current value. Returns newval."
  [k newval]
  (r/wcar conn (r/set k newval)) ;; need to check for sucess;
  newval)

(defn swap!
  "Atomically swaps the value of atom to be:
  (apply f current-value-of-atom args). Note that f may be called
  multiple times, and thus should be free of side effects.  Returns
  the value that was swapped in."
  [k f & args]
  (r/wcar conn
    (loop [oldval (r/get k)]
      (let [newval (apply f oldval args)]
        (if (compare-and-set! k oldval newval)
          newval
          (recur (r/get k)))))))
