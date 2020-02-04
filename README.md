# redis-atom
Share clojure atoms between services via redis with one line of code.

[![Clojars Project](https://img.shields.io/clojars/v/redis-atom.svg)](https://clojars.org/redis-atom)

## Using
Define redis connection spec like with [`taoensso.carmine`](https://github.com/ptaoussanis/carmine). Define an atom by passing in the connetion spec, a key that will point to the atom value on Redis and the atom value. Note that if the input key already exists on redis backend then the input value will *not* be assigned to it. Everything else is exactly the same as with Clojure atoms.

```clojure
(require '[redis-atom.core :refer [redis-atom]])

(def conn {:pool {} :spec {:uri "redis://localhost:6379"}})

(def a (redis-atom conn :redis-key {:my-data "42" :more-data 43}))

a ; => #object[redis_atom.core.RedisAtom 0x471a378 {:status :ready, :val {:my-data "42", :more-data 43}}]
@a ; => {:my-data "42" :more-data 43}

(def b (redis-atom conn :redis-key 42))
@b ; => {:my-data "42" :more-data 43}

(reset! a 42) ; => 42
@b ; => 42

(reset-vals! a 43) ; => [42 43]
(swap! a inc) ; => 44
(swap-vals! a inc) ; => [44 45]
```

## Testing
Running the test suite requires redis backend service which can be easily created with [docker-compose](https://docs.docker.com/compose/install/).
To start a local backend:
```shell
$ cd redis
$ docker-compose up -d
```
This will start a redis server on `6379` and a redis-commander on `8081`. If you are a fan of redis-cli, run `redis-cli.sh` script in the same dir for the console.
