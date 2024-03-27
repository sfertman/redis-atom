(ns redis-atom.redis
  (:require [taoensso.carmine :as r]))

(defn deref* [conn k] (:data (r/wcar conn (r/get k))))

(defn reset* [conn k newval] (r/wcar conn (r/set k {:data newval})))

(defn setnx* [conn k newval] (r/wcar conn (r/setnx k {:data newval})))

(def lkey (partial r/key :lock))

(defn acquire-lock
  "Attempts to acquire a distributed lock, returning an owner UUID iff successful."
  [conn lock-name timeout-s wait-s]
  (let [max-udt (+ (* wait-s 1000) (System/currentTimeMillis))
        uuid    (str (java.util.UUID/randomUUID))
        timeout-ms (* timeout-s 1000)]
    (r/wcar conn
     (loop []
       (when (> max-udt (System/currentTimeMillis))
         (if (= "OK"
                (-> (r/set (lkey lock-name) uuid :px timeout-ms :nx)
                    r/with-replies))
           (r/return uuid)
           (do (Thread/sleep 1) (recur))))))))

(defn release-lock
  "Attempts to release a distributed lock, returning true iff successful."
  [conn lock-name owner-uuid]
  (r/wcar conn
   (r/parse-bool
    (r/lua
     "if redis.call('get', _:lkey) == _:uuid then
         redis.call('del', _:lkey);
         return 1;
       else
         return 0;
       end"
     {:lkey (lkey lock-name)}
     {:uuid owner-uuid}))))

(defn lock-set [conn k validator f args]
  (let [lock-uuid
        (acquire-lock conn k 5 5)]
    (when-not lock-uuid
      (throw (ex-info "Redis lock could not be acquired!" {:lock-key k})))
    (try
      (let [old-value
            (deref* conn k)

            new-value
            (apply f old-value args)]
        (when validator
          (validator new-value))

        (reset* conn k new-value)
        [old-value new-value])
      (finally
        (when-not (release-lock conn k lock-uuid)
          (throw (ex-info "Redis lock had a different owner!" {:lock-key k})))))))

;; NOTE unwatch and exec will remove all watches in the client. Since Carmine
;; uses a connection pool, this means that using Redis transactions is only safe
;; when each transaction uses a different client/connection. As a result, we
;; implemented an alternative mechanism using locks
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
