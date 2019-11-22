(ns redis-atom.core
  (:require [redis-atom.RedisAtom]))

(defn redis-atom
  ([conn k val] (let [a (RedisAtom. conn k)] (.reset a val) a))
  ([conn k val & {opts :options vtor :validator mta :meta}]
    (let [a (RedisAtom. conn k opts mta)]
      (.reset a val)
      (when vtor (.setValidator a vtor))
      a)))
