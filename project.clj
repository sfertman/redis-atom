(defproject redis-atom "1.1.1"
  :description "Share clojure atoms between services via redis with one line of code"
  :license {
    :name "MIT"
    :url "https://opensource.org/licenses/mit-license.php"}
  :dependencies [
    [com.taoensso/carmine "2.19.1"]
    [org.clojure/clojure "1.10.0"]]
  :repl-options {:init-ns redis-atom.core}
  :aot [redis-atom.RedisAtom])
