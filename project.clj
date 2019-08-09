(defproject redis-atom "0.1.0-SNAPSHOT"
  :description "Just like clojure's atom but on Redis backend"
  :license {
    :name "MIT"
    :url "https://opensource.org/licenses/mit-license.php"}
  :dependencies [
    [org.clojure/clojure "1.10.0"]]
  :repl-options {:init-ns redis-atom.core})
