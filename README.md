# redis-atom
Share atoms between services. 

## Supported for now
- atom
- deref
- reset!
- swap!
- compare-and-set!

## Using
Define redis connection spec like with [`taoensso.carmine`](https://github.com/ptaoussanis/carmine). Define an atom by passing in the connetion spec, a key that will point to the atom value on Redis and the atom value. Everything else is the same as with Clojure atoms (almost).

```clojure
(require '[redis-atom.core :as r])

(def conn {:pool {} :spec {:uri "redis://localhost:6389"}})

(def a (r/atom conn :redis-key {:my-data "42" :more-data 43}))
; => {:conn {...} :key :redis-key}

(r/deref a) ; => {:my-data "42" :more-data 43}

(r/reset! a 42) ; => 42

(r/deref a) ; => 42

(r/swap! a inc) ; => 43

(r/deref a) ; => 43
```

## Testing
Requires docker and docker-compose.

Start a local backend:
```shell
$ cd redis
$ docker-compose up -d
```
This will start a redis server on `6379` and a redis-commander on `8081`. If you are a `redis-cli` kind of person, run `redis-cli.sh` script in the same dir for the console.
