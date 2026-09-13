sdk use java 17.0.9-graalce
./mvnw package -Pnative
cd rest-heroes
docker build -f src/main/docker/Dockerfile.native -t flyaruu/rest-heroes-native:latest .
cd ../rest-villains
docker build -f src/main/docker/Dockerfile.native -t flyaruu/rest-villains-native:latest .
cd ../grpc-locations
docker build -f src/main/docker/Dockerfile.native -t flyaruu/grpc-locations-native:latest .
cd ../rest-fights
docker build -f src/main/docker/Dockerfile.native -t flyaruu/rest-fights-native:latest .
cd ..

# sdk use java 17.0.9-graalce
# ./mvnw package -Pnative
# docker build -f src/main/docker/Dockerfile.native -t flyaruu/rest-heroes-native .
