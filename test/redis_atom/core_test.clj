(ns redis-atom.core-test
  (:require
    [clojure.pprint :refer [pprint]]
    [clojure.test :refer :all]
    [redis-atom.core :as r]
    [taoensso.carmine :as redis]))


(def conn {
  :pool {}
  :spec {:uri "redis://localhost:6379"}})

(def a (r/atom conn :k nil ))

(pprint (r/reset! a {} ))

(pprint (r/deref a))

(pprint (r/reset! a {:a 42 :b 24}))

(pprint (r/swap! a inc))
(pprint (r/swap! a + 5))