(ns redis-atom.core
  (:require [redis-atom.RedisAtom]))

(defn redis-atom
  ([conn val] (let [a (RedisAtom. conn)] (.reset a val) a))
  ([conn val & {mta :meta v-tor :validator}]
    (let [a (redis-atom conn val)]
      (when mta (.resetMeta a mta))
      (when v-tor (.setValidator a v-tor))
      a)))