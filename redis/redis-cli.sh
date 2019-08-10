#!/bin/sh
docker run --rm -it \
  --name redis_atom_cli \
  --network redis_atom_net \
  redis:alpine redis-cli -h redis_backend