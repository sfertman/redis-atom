(ns redis-atom.redis
  (:require [taoensso.carmine :as r]))

(defn deref* [conn k] (:data (r/wcar conn (r/get k))))

(defn reset* [conn k newval] (r/wcar conn (r/set k {:data newval})))

(defn setnx* [conn k newval] (r/wcar conn (r/setnx k {:data newval})))

(defn compare-and-set* [conn k oldval newval]
  (r/wcar conn (r/watch k))
  (if (not= oldval (deref* conn k))
    (do (r/wcar conn (r/unwatch))
        false)
    #_else
    ;; carmine returns a result for each command, so if everything works as
    ;; expected, it will return
    ;; ["OK" "QUEUED" ["OK"]]
    ;; being the last result the outcome of the queued commands.
    (= ["OK"] (last
               (r/wcar conn
                       (r/multi)
                       (r/set k {:data newval})
                       (r/exec))))))
