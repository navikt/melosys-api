#!/bin/sh
if test -f /var/run/secrets/nais.io/serviceuser/username
then
  export SRV_USERNAME="$(cat /var/run/secrets/nais.io/serviceuser/username)"
  export SRV_PASSWORD="$(cat /var/run/secrets/nais.io/serviceuser/password)"
fi
