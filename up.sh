#!/bin/sh
docker compose -f deploy/docker-compose/java21.yml up --force-recreate -d
sleep 30
