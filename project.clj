(defproject redis-atom "1.1.0"
  :description "Share clojure atoms between services via redis with one line of code"
  :license {
    :name "MIT"
    :url "https://opensource.org/licenses/mit-license.php"}
  :dependencies [
    [com.taoensso/carmine "2.19.1"]
    [org.clojure/clojure "1.10.0"]
    [org.clojure/core.async "0.4.500"]]
  :repl-options {:init-ns redis-atom.core}
  :aot [redis-atom.RedisAtom])
