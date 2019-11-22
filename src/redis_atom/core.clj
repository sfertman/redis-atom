(ns redis-atom.core
  (:require [redis-atom.RedisAtom]))

(defn redis-atom
  ([conn k val] (let [a (RedisAtom. conn k)] (.reset a val) a))
  ([conn k val & {mta :meta v-tor :validator}]
    (let [a (redis-atom conn k val)]
      (when mta (.resetMeta a mta))
      (when v-tor (.setValidator a v-tor))
      a)))