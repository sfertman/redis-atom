# redis-atom
Share atoms between services.

## Supported for now
- atom
- deref
- reset!
- swap!
- compare-and-set!

## Using
Define redis connection spec like with [`taoensso.carmine`](https://github.com/ptaoussanis/carmine). Define an atom by passing in the connetion spec, a key that will point to the atom value on Redis and the atom value. Everything else is exactly the same as with Clojure atoms.

```clojure
(require '[redis-atom.core :refer [redis-atom]])

(def conn {:pool {} :spec {:uri "redis://localhost:6379"}})

(def a (redis-atom conn :redis-key {:my-data "42" :more-data 43}))

a ; => #object[redis_atom.core.RedisAtom 0x471a378 {:status :ready, :val {:my-data "42", :more-data 43}}]
(deref a) ; => {:my-data "42" :more-data 43}

(reset! a 42) ; => 42

@a ; => 42

(swap! a inc) ; => 43

@a ; => 43
```

## Testing
Requires docker and docker-compose.

Start a local backend:
```shell
$ cd redis
$ docker-compose up -d
```
This will start a redis server on `6379` and a redis-commander on `8081`. If you are a `redis-cli` kind of person, run `redis-cli.sh` script in the same dir for the console.
