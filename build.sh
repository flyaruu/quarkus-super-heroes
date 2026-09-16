#!/bin/bash

set -e

sdk use java 21.0.12+1.1-tem
./mvnw package
cd rest-heroes
docker build -f src/main/docker/Dockerfile.jvm -t flyaruu/rest-heroes-jvm21:latest .
docker push flyaruu/rest-heroes-jvm21:latest
cd ../rest-villains
docker build -f src/main/docker/Dockerfile.jvm -t flyaruu/rest-villains-jvm21:latest .
docker push flyaruu/rest-villains-jvm21:latest
cd ../grpc-locations
docker build -f src/main/docker/Dockerfile.jvm -t flyaruu/grpc-locations-jvm21:latest .
docker push flyaruu/grpc-locations-jvm21:latest
cd ../rest-fights
docker build -f src/main/docker/Dockerfile.jvm -t flyaruu/rest-fights-jvm21:latest .
docker push flyaruu/rest-fights-jvm21:latest
cd ..

# sdk use java 17.0.9-graalce
# ./mvnw package -Pnative
# docker build -f src/main/docker/Dockerfile.native -t flyaruu/rest-heroes-native .
