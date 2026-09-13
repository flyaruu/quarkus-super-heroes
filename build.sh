sdk use java 21.0.12+1.1-tem
./mvnw package
cd rest-heroes
docker build -f src/main/docker/Dockerfile.jvm -t flyaruu/rest-heroes .
cd ../rest-villains
docker build -f src/main/docker/Dockerfile.jvm -t flyaruu/rest-villains .
cd ../grpc-locations
docker build -f src/main/docker/Dockerfile.jvm -t flyaruu/grpc-locations .
cd ../rest-fights
docker build -f src/main/docker/Dockerfile.jvm -t flyaruu/rest-fights .
cd ..

# sdk use java 17.0.9-graalce
# ./mvnw package -Pnative
# docker build -f src/main/docker/Dockerfile.native -t flyaruu/rest-heroes-native .
