---
title: Location gRPC API
description: A gRPC microservice written in Kotlin exposing CRUD operations on Locations, stored in a MariaDB database.
layout: page
content-toc: true
---

## Introduction

This is the Location gRPC microservice. It is a classical gRPC microservice, written in [Kotlin](https://quarkus.io/guides/kotlin), and exposing CRUD operations on Locations. Location information is stored in a MariaDB database. This service is implemented using [gRPC](https://quarkus.io/guides/grpc-service-implementation) with blocking endpoints and [Quarkus Hibernate ORM with Panache's repository pattern](https://quarkus.io/guides/hibernate-orm-panache-kotlin#using-the-repository-pattern).

Additionally, this application favors constructor injection of beans over field injection (i.e. `@Inject` annotation).

![grpc-locations]({site.image('grpc-locations.png')})

### Exposed Endpoints

Since this is a gRPC service, the endpoints are defined via a protobuf definition. The protobuf file can be found at [`locationservice-v1.proto`](https://github.com/quarkusio/quarkus-super-heroes/blob/main/grpc-locations/src/main/proto/locationservice-v1.proto) in the source repository.

## Contract testing with Pact

[Pact](https://pact.io) is a code-first tool for testing HTTP and message integrations using `contract tests`. Contract tests assert that inter-application messages conform to a shared understanding that is documented in a contract. Without contract testing, the only way to ensure that applications will work correctly together is by using expensive and brittle integration tests.

[Eric Deandrea](https://developers.redhat.com/author/eric-deandrea) and [Holly Cummins](https://hollycummins.com) recently spoke about contract testing with Pact and used the Quarkus Superheroes for their demos. [Watch the replay](https://www.youtube.com/watch?v=vYwkDPrzqV8) and [view the slides](https://hollycummins.com/modern-microservices-testing-pitfalls-devoxx/) if you'd like to learn more about contract testing.

The `grpc-locations` application is a [Pact _Provider_](https://docs.pact.io/provider), and as such, should run provider verification tests against contracts produced by consumers.

**NOTE:** The `grpc-locations` service uses gRPC/protobuf and not REST, therefore the consumer contract tests between [rest-fights]({site.url('/rest-fights')}) and `grpc-locations` use the [Pact protobuf plugin](https://docs.pact.io/implementation_guides/pact_plugins/plugins/protobuf). There is no installation necessary. When the tests execute the plugin will be automatically installed.

Contracts generally should be hosted in a [Pact Broker](https://docs.pact.io/pact_broker) and then automatically discovered in the provider verification tests. One of the main goals of the Superheroes application is to be super simple and just "work" by anyone who may clone the repo. Therefore, the Pact contract is committed into the application's source tree inside the `src/test/resources/pacts` directory. In a realistic scenario, if a broker wasn't used, the consumer's CI/CD would commit the contracts into this repository's source control.

The Pact tests use the [Quarkus Pact extension](https://github.com/quarkiverse/quarkus-pact). This extension is recommended to give the best user experience and ensure compatibility.

## Benchmarking with Hyperfoil

[Hyperfoil](https://hyperfoil.io) doesn't yet support gRPC. [This issue](https://github.com/Hyperfoil/Hyperfoil/issues/281) is currently tracking it.

## Running the Application

The application runs on port `8089` (defined by `quarkus.http.port` in `application.yml`).

From the `quarkus-super-heroes/grpc-locations` directory, simply run `./mvnw quarkus:dev` to run [Quarkus Dev Mode](https://quarkus.io/guides/maven-tooling#dev-mode), or running `quarkus dev` using the [Quarkus CLI](https://quarkus.io/guides/cli-tooling). The application will be exposed at http://localhost:8089 and the [Quarkus Dev UI](https://quarkus.io/guides/dev-ui) will be exposed at http://localhost:8089/q/dev.

**NOTE:** Running the application outside of Quarkus dev mode requires standing up a MariaDB instance and binding it to the app. By default, the application is configured with the following:

| Description | Environment Variable | Java Property | Value |
|---|---|---|---|
| Database URL | `QUARKUS_DATASOURCE_JDBC_URL` | `quarkus.datasource.jdbc.url` | `jdbc:mariadb://locations-db:3306/locations_database` |
| Database username | `QUARKUS_DATASOURCE_USERNAME` | `quarkus.datasource.username` | `locations` |
| Database password | `QUARKUS_DATASOURCE_PASSWORD` | `quarkus.datasource.password` | `locations` |

## Running Locally via Docker Compose

Pre-built images for this application can be found at [`quay.io/quarkus-super-heroes/grpc-locations`](https://quay.io/repository/quarkus-super-heroes/grpc-locations?tab=tags).

Pick one of the versions of the application from the table below and execute the appropriate docker compose command from the `quarkus-super-heroes/grpc-locations` directory.

**NOTE:** You may see errors as the applications start up. This may happen if an application completes startup before one of its required services (i.e. database, etc). This is fine. Once everything completes startup things will work fine.

| Description | Image Tag | Docker Compose Run Command |
|---|---|---|
| JVM Java 25 | `java25-latest` | `docker compose -f deploy/docker-compose/java25.yml up --remove-orphans` |
| Native | `native-latest` | `docker compose -f deploy/docker-compose/native.yml up --remove-orphans` |

These Docker Compose files are meant for standing up this application and the required database only. If you want to stand up the entire system, follow the instructions in the main project README.

Once started the application will be exposed at `http://localhost:8089`.
