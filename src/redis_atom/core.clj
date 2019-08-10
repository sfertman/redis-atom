(ns redis-atom.core
  (:require
    [taoensso.carmine :as car :refer [wcar]]))

(def conn {})

(defn compare-and-set!
  "Atomically sets the value of atom to newval if and only if the
  current value of the atom is identical to oldval. Returns true if
  set happened, else false"
  [k oldval newval]
  (car/watch k)
  (if (not= oldval (car/get k))
    (do
      (car/unwatch)
      false)
    (do
      (car/multi)
      (car/set k newval)
      (some? (car/exec))))) ;; exec will return nil if failed

(defn atom
  [k val]
  (wcar conn (car/set k val)) ;; need to check for success
  k)

(defn reset!
  "Sets the value of atom to newval without regard for the
  current value. Returns newval."
  [k newval]
  (wcar conn (car/set k newval)) ;; need to check for sucess;
  newval)

(defn swap!
  "Atomically swaps the value of atom to be:
  (apply f current-value-of-atom args). Note that f may be called
  multiple times, and thus should be free of side effects.  Returns
  the value that was swapped in."
  [k f & args]
  (wcar conn
    (loop [oldval (car/get k)]
      (let [newval (apply f oldval args)]
        (if (compare-and-set! k oldval newval)
          newval
          (recur (car/get k)))))))
