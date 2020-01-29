(ns redis-atom.core
  (:require
    [redis-atom.RedisAtom]
    [redis-atom.redis :refer [setnx*]]))

(defn redis-atom
  ([conn k] (RedisAtom. conn k))
  ([conn k val] (let [a (redis-atom conn k)] (setnx* conn k val) a))
  ([conn k val & {mta :meta v-tor :validator}]
    (let [a (redis-atom conn k val)]
      (when mta (.resetMeta a mta))
      (when v-tor (.setValidator a v-tor))
      a)))